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

package geotrellis.spark.store

import geotrellis.proj4.LatLng
import geotrellis.vector._
import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.raster.testkit._
import geotrellis.layers.{TileLayerMetadata, LayerOutOfKeyBoundsError}
import geotrellis.spark._
import geotrellis.spark.util._
import geotrellis.util._
import geotrellis.spark.testkit._
import geotrellis.spark.testkit.io._

import java.time.{ZoneOffset, ZonedDateTime}


trait LayerUpdateSpaceTimeTileFeatureSpec
    extends TileLayerRDDBuilders
    with TileBuilders { self: PersistenceSpec[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]] with TestEnvironment =>

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

  for(PersistenceSpecDefinition(keyIndexMethodName, keyIndexMethod, layerIds) <- specLayerIds) {
    val layerId = layerIds.layerId

    describe(s"updating for $keyIndexMethodName") {
      it("should update a layer") {
        writer.update(layerId, sample)
      }

      it("should not update a layer (empty set)") {
        // expect log.warn and no exception
        writer.update(layerId, new ContextRDD[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](sc.emptyRDD[(SpaceTimeKey, Tile)], emptyTileLayerMetadata))
      }

      it("should not update a layer (keys out of bounds)") {
        val (minKey, minTile) = sample.sortByKey().first()
        val (maxKey, maxTile) = sample.sortByKey(false).first()

        val update = new ContextRDD(sc.parallelize(
          (minKey.setComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
            (minKey.setComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
        ), dummyTileLayerMetadata)

        intercept[LayerOutOfKeyBoundsError] {
          writer.update(layerId, update)
        }
      }

      it("should update a layer with preset keybounds, new rdd not intersects already ingested") {
        val (minKey, _) = sample.sortByKey().first()
        val (maxKey, _) = sample.sortByKey(false).first()
        val kb = KeyBounds(minKey, maxKey.setComponent(SpatialKey(maxKey.col + 20, maxKey.row + 20)))
        val updatedLayerId = layerId.createTemporaryId
        val updatedKeyIndex = keyIndexMethod.createIndex(kb)

        val usample = sample.map { case (key, value) => (key.setComponent(SpatialKey(key.col + 10, key.row + 10)), value) }
        val ukb = KeyBounds(usample.sortByKey().first()._1, usample.sortByKey(false).first()._1)
        val updatedSample = new ContextRDD(usample, sample.metadata.copy(bounds = ukb))

        writer.write[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]](updatedLayerId, sample, updatedKeyIndex)
        writer.update[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]](updatedLayerId, updatedSample)
        reader.read[SpaceTimeKey, TileFeature[Tile, Tile], TileLayerMetadata[SpaceTimeKey]](updatedLayerId).count() shouldBe sample.count() * 2
      }

      it("should update correctly inside the bounds of a metatile") {
        val id = layerId.createTemporaryId

        val tiles =
          Seq(
            (createValueTile(6, 4, 1), ZonedDateTime.of(2016, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(6, 4, 2), ZonedDateTime.of(2016, 1, 2, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(6, 4, 3), ZonedDateTime.of(2016, 1, 3, 12, 0, 0, 0, ZoneOffset.UTC)),
            (createValueTile(6, 4, 4), ZonedDateTime.of(2016, 1, 4, 12, 0, 0, 0, ZoneOffset.UTC))
          )

        val rdd =
          createSpaceTimeTileLayerRDD(
            tiles,
            TileLayout(1, 1, 6, 4)
          )

        assert(rdd.count == 4)

        writer.write(id, rdd, keyIndexMethod)
        assert(reader.read[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](id).count == 4)

        val updateRdd =
          createSpaceTimeTileLayerRDD(
            Seq((createValueTile(6, 4, 5), ZonedDateTime.of(2016, 1, 4, 12, 0, 0, 0, ZoneOffset.UTC))),
            TileLayout(1, 1, 6, 4)
          )

        assert(updateRdd.count == 1)
        updateRdd.withContext(_.mapValues { tile => tile + 1 })

        writer.update[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](id, updateRdd)
        val read: TileLayerRDD[SpaceTimeKey] = reader.read(id)

        val readTiles = read.collect.sortBy { case (k, _) => k.instant }.toArray
        readTiles.size should be (4)
        assertEqual(readTiles(0)._2, Array(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        assertEqual(readTiles(1)._2, Array(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))
        assertEqual(readTiles(2)._2, Array(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
        assertEqual(readTiles(3)._2, Array(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      }
    }
  }
}
