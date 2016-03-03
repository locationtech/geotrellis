package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.json._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4.LatLng

import org.apache.spark.rdd._
import org.joda.time.DateTime

trait LayerUpdateSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData[SpaceTimeKey]] with TestEnvironment =>

  def updater: LayerUpdater[LayerId]

  def dummyRasterMetaData =
    RasterMetaData(
      IntConstantNoDataCellType,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng,
      KeyBounds(SpaceTimeKey(0,0, new DateTime()), SpaceTimeKey(1,1, new DateTime()))
    )

  def emptyRasterMetaData =
    RasterMetaData[SpaceTimeKey](
      IntConstantNoDataCellType,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng,
      EmptyBounds
    )

  addSpecs { layerIds =>
    val layerId = layerIds.layerId

    it("should update a layer") {
      updater.update(layerId, sample)
    }

    it("should not update a layer (empty set)") {
      intercept[LayerUpdateError] {
        updater.update(layerId, new ContextRDD[SpaceTimeKey, Tile, RasterMetaData[SpaceTimeKey]](sc.emptyRDD[(SpaceTimeKey, Tile)], emptyRasterMetaData))
      }
    }

    it("should not update a layer (keys out of bounds)") {
      val (minKey, minTile) = sample.sortByKey().first()
      val (maxKey, maxTile) = sample.sortByKey(false).first()

      val update = new ContextRDD(sc.parallelize(
        (minKey.setComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
          (minKey.setComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
      ), dummyRasterMetaData)

      intercept[LayerOutOfKeyBoundsError] {
        updater.update(layerId, update)
      }
    }
  }
}
