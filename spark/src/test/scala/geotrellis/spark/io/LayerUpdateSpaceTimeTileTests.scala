package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.vector._
import geotrellis.proj4.LatLng

trait LayerUpdateSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile, RasterMetadata] with TestEnvironment =>

  def updater: TestUpdater

  def dummyRasterMetadata: RasterMetadata =
    RasterMetadata(
      TypeInt,
      LayoutDefinition(RasterExtent(Extent(0,0,1,1), 1, 1), 1),
      Extent(0,0,1,1),
      LatLng
    )

  it("should update a layer") {
    updater.update(layerId, sample)
  }

  it("should not update a layer (empty set)") {
    intercept[LayerUpdateError] {
      updater.update(layerId, new ContextRDD[SpaceTimeKey, Tile, RasterMetadata](sc.emptyRDD[(SpaceTimeKey, Tile)], dummyRasterMetadata))
    }
  }

  it("should not update a layer (keys out of bounds)") {
    val (minKey, minTile) = sample.sortByKey().first()
    val (maxKey, maxTile) = sample.sortByKey(false).first()

    val update = new ContextRDD(sc.parallelize(
      (minKey.updateSpatialComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
        (minKey.updateSpatialComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
    ), dummyRasterMetadata)

    intercept[LayerOutOfKeyBoundsError] {
      updater.update(layerId, update)
    }
  }
}
