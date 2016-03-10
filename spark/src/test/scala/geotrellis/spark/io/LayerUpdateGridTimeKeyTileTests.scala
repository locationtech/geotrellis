package geotrellis.spark.io

import geotrellis.raster.testkit._
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._
import geotrellis.spark.testkit._
import geotrellis.spark.tiling._
import geotrellis.spark.util._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4.LatLng

import org.apache.spark.rdd._
import org.joda.time.DateTime

trait LayerUpdateGridTimeKeyTileTests extends RasterRDDBuilders with TileBuilders { self: PersistenceSpec[GridTimeKey, Tile, RasterMetadata[GridTimeKey]] with TestEnvironment =>

  def dummyRasterMetadata =
    RasterMetadata(
      IntConstantNoDataCellType,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng,
      KeyBounds(GridTimeKey(0,0, new DateTime()), GridTimeKey(1,1, new DateTime()))
    )

  def emptyRasterMetadata =
    RasterMetadata[GridTimeKey](
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
        updater.update(layerId, sample)
      }

      it("should not update a layer (empty set)") {
        intercept[EmptyBoundsError] {
          updater.update(layerId, new ContextRDD[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](sc.emptyRDD[(GridTimeKey, Tile)], emptyRasterMetadata))
        }
      }

      it("should not update a layer (keys out of bounds)") {
        val (minKey, minTile) = sample.sortByKey().first()
        val (maxKey, maxTile) = sample.sortByKey(false).first()

        val update = new ContextRDD(sc.parallelize(
          (minKey.setComponent(GridKey(minKey.col - 1, minKey.row - 1)), minTile) ::
            (minKey.setComponent(GridKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
        ), dummyRasterMetadata)

        intercept[LayerOutOfKeyBoundsError] {
          updater.update(layerId, update)
        }
      }

      it("should update correctly inside the bounds of a metatile") {
        val id = layerId.createTemporaryId

        val tiles =
          Seq(
            (createValueTile(6, 4, 1), new DateTime(2016, 1, 1, 12, 0, 0)),
            (createValueTile(6, 4, 2), new DateTime(2016, 1, 2, 12, 0, 0)),
            (createValueTile(6, 4, 3), new DateTime(2016, 1, 3, 12, 0, 0)),
            (createValueTile(6, 4, 4), new DateTime(2016, 1, 4, 12, 0, 0))
          )

        val rdd =
          createGridTimeKeyRasterRDD(
            tiles,
            TileLayout(1, 1, 6, 4)
          )

        assert(rdd.count == 4)

        writer.write(id, rdd, keyIndexMethod)
        assert(reader.read[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](id).count == 4)

        val updateRdd =
          createGridTimeKeyRasterRDD(
            Seq((createValueTile(6, 4, 5), new DateTime(2016, 1, 4, 12, 0, 0))),
            TileLayout(1, 1, 6, 4)
          )

        assert(updateRdd.count == 1)
        updateRdd.withContext(_.mapValues { tile => tile + 1 })

        updater.update[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](id, updateRdd)

        val read: RasterRDD[GridTimeKey] = reader.read(id)

        val readTiles = read.collect.sortBy { case (k, _) => k.instant }.toArray
        readTiles.size should be (4)
        println(readTiles(0)._2.toArray.toSeq)
        assertEqual(readTiles(0)._2, Array(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1))
        assertEqual(readTiles(1)._2, Array(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2))
        assertEqual(readTiles(2)._2, Array(3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3))
        assertEqual(readTiles(3)._2, Array(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5))
      }
    }
  }
}
