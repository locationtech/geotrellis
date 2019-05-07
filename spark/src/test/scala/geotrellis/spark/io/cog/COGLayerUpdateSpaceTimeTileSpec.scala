/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.io.cog

import geotrellis.proj4.LatLng
import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.layers._
import geotrellis.layers.cog._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.io.cog._
import geotrellis.util._

import java.time._


trait COGLayerUpdateSpaceTimeTileSpec
    extends TileLayerRDDBuilders
    with TileBuilders { self: COGPersistenceSpec[SpaceTimeKey, Tile] with TestEnvironment =>

  implicit class withLayerIdUtilMethods(val self: LayerId) extends MethodExtensions[LayerId] {
    def createTemporaryId(): LayerId = self.copy(name = s"${self.name}-update-${ZonedDateTime.now.toInstant.toEpochMilli}")
  }

  def dummyTileLayerMetadata =
    TileLayerMetadata(
      IntConstantNoDataCellType,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng,
      KeyBounds(SpaceTimeKey(0,0, ZonedDateTime.now), SpaceTimeKey(1,1, ZonedDateTime.now))
    )

  def emptyTileLayerMetadata =
    TileLayerMetadata[SpaceTimeKey](
      IntConstantNoDataCellType,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng,
      EmptyBounds
    )

  def mergeFunc: Option[(GeoTiff[Tile], GeoTiff[Tile]) => GeoTiff[Tile]] = Some({ case (l, r) => COGLayer.mergeCOGs(l, r) })

  for(PersistenceSpecDefinition(keyIndexMethodName, keyIndexMethod, layerIds) <- specLayerIds) {
    val layerId = layerIds.layerId

    describe(s"updating for $keyIndexMethodName") {
      it("should update a layer") {
        writer.update(layerId.name, sample, layerId.zoom, mergeFunc = mergeFunc)
      }

      it("should overwrite a layer") {
        writer.overwrite(layerId.name, sample, layerId.zoom)
      }

      it("should not update a layer (empty set)") {
        intercept[EmptyBoundsError] {
          writer.update(layerId.name, new ContextRDD[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](sc.emptyRDD[(SpaceTimeKey, Tile)], emptyTileLayerMetadata), layerId.zoom, mergeFunc = mergeFunc)
        }
      }

      it("should silently not overwrite a layer (empty set)") {
        writer.overwrite(layerId.name, new ContextRDD[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](sc.emptyRDD[(SpaceTimeKey, Tile)], emptyTileLayerMetadata), layerId.zoom)
      }

      it("should not update a layer (keys out of bounds)") {
        val (minKey, minTile) = sample.sortByKey().first()
        val (maxKey, maxTile) = sample.sortByKey(false).first()

        val update = new ContextRDD(sc.parallelize(
          (minKey.setComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
            (minKey.setComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
        ), dummyTileLayerMetadata)

        intercept[LayerOutOfKeyBoundsError] {
          writer.update(layerId.name, update, layerId.zoom, mergeFunc = mergeFunc)
        }
      }

      it("should not overwrite a layer (keys out of bounds)") {
        val (minKey, minTile) = sample.sortByKey().first()
        val (maxKey, maxTile) = sample.sortByKey(false).first()

        val update = new ContextRDD(sc.parallelize(
          (minKey.setComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
          (minKey.setComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
        ), dummyTileLayerMetadata)

        intercept[LayerOutOfKeyBoundsError] {
          writer.overwrite(layerId.name, update, layerId.zoom)
        }
      }

      it("should update a layer with preset keybounds, new rdd not intersects already ingested") {
        /**
          * !!IMPORTANT: we need to have keyBounds starting from (0, 0), as COGs are stored in TIFFs and
          * it can't be sparsed => it is expected to have a significant amount of empty tiles.
          */
        val minKey = {
          val k = sample.sortByKey().first()._1
          SpaceTimeKey(0, 0, k.instant)
        }
        val (maxKey, _) = sample.sortByKey(false).first()
        val kb = KeyBounds(minKey, maxKey.setComponent(SpatialKey(maxKey.col + 20, maxKey.row + 20)))
        val updatedLayerId = layerId.createTemporaryId
        val updatedKeyIndex = keyIndexMethod.createIndex(kb)

        val usample = sample.map { case (key, value) => (key.setComponent(SpatialKey(key.col + 10, key.row + 10)), value) }
        val ukb = KeyBounds(usample.sortByKey().first()._1, usample.sortByKey(false).first()._1)

        val updatedSample = new ContextRDD(
          usample,
          sample
            .metadata
            .copy(bounds = ukb)
            .copy(extent = sample.metadata.mapTransform(ukb.toGridBounds).bufferByLayout(sample.metadata.layout))
        )

        writer.write[SpaceTimeKey, Tile](updatedLayerId.name, sample, updatedLayerId.zoom, updatedKeyIndex)
        writer.update[SpaceTimeKey, Tile](updatedLayerId.name, updatedSample, updatedLayerId.zoom, mergeFunc = mergeFunc)

        /** !!IMPORTANT: the place where empty tiles are filtered out */
        val resultKeys = reader.read[SpaceTimeKey, Tile](updatedLayerId).filter(!_._2.isNoDataTile).map(_._1).collect.toList
        val sampleKeys = sample.map(_._1).collect().toList
        val udpatedSampleKeys = updatedSample.map(_._1).collect.toList

        resultKeys should contain theSameElementsAs (udpatedSampleKeys ++ sampleKeys)
        resultKeys.length shouldBe sampleKeys.length * 2
      }

      it("should update correctly inside the bounds of a metatile") {
        val tileLayout = TileLayout(8, 8, 4, 4)
        val id = layerId.createTemporaryId

        val tiles =
          Seq(
            (createValueTile(4, 4, 1), ZonedDateTime.of(2014, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(4, 4, 2), ZonedDateTime.of(2015, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(4, 4, 3), ZonedDateTime.of(2016, 1, 3, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(4, 4, 4), ZonedDateTime.of(2017, 1, 4, 12, 0, 0, 0, ZoneOffset.UTC))
          )

        /** !!IMPORTANT: the place where empty tiles are filtered out, due to the same reason as in previous test */
        val rdd = createSpaceTimeTileLayerRDD(tiles, tileLayout).withContext { _.filter(!_._2.isNoDataTile) }
        assert(rdd.count == 4)

        writer.write(id.name, rdd, id.zoom, keyIndexMethod)

        val updateRdd =
          createSpaceTimeTileLayerRDD(
            Seq((createValueTile(4, 4, 5), ZonedDateTime.of(2017, 1, 4, 12, 0, 0, 0, ZoneOffset.UTC))),
            tileLayout
          ).withContext { _.filter(!_._2.isNoDataTile) }

        assert(updateRdd.count == 1)

        updateRdd.withContext(_.mapValues { tile => tile + 1 })
        writer.update[SpaceTimeKey, Tile](id.name, updateRdd, id.zoom, mergeFunc = mergeFunc)
        val read: TileLayerRDD[SpaceTimeKey] = reader.read[SpaceTimeKey, Tile](id).withContext { _.filter(!_._2.isNoDataTile) }

        val readTiles = read.collect.sortBy { case (k, _) => k.instant }.toArray
        readTiles.size should be (4)
        assertEqual(readTiles(0)._2, Array(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        assertEqual(readTiles(1)._2, Array(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))
        assertEqual(readTiles(2)._2, Array(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
        assertEqual(readTiles(3)._2, Array(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      }
    }
  }
}
