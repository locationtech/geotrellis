package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark.{SpaceTimeKey, TestSparkContext, SpatialKey}

trait LayerUpdateSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile] with TestSparkContext =>

  def updater: TestUpdater

  it("should update a layer") {
    updater.update(layerId, sample)
  }

  it("should not update a layer (empty set)") {
    intercept[LayerUpdateError] {
      updater.update(layerId, sc.emptyRDD[(SpaceTimeKey, Tile)].asInstanceOf[Container])
    }
  }

  it("should not update a layer (keys out of bounds)") {
    val (minKey, minTile) = sample.sortByKey().first()
    val (maxKey, maxTile) = sample.sortByKey(false).first()

    val update = sc.parallelize(
      (minKey.updateSpatialComponent(SpatialKey(minKey.col - 1, minKey.row - 1)), minTile) ::
        (minKey.updateSpatialComponent(SpatialKey(maxKey.col + 1, maxKey.row + 1)), maxTile) :: Nil
    ).asInstanceOf[Container]

    intercept[LayerOutOfKeyBoundsError] {
      updater.update(layerId, update)
    }
  }
}
