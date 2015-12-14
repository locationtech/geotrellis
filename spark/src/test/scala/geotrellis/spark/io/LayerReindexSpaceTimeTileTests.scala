package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark._

trait LayerReindexSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile] with TestSparkContext =>

  def reindexer: TestReindexer
  val reindexedLayerId = LayerId("reindexedSample-" + this.getClass.getName, 1)

  it("should not reindex a layer which doesn't exists") {
    intercept[LayerNotFoundError] {
      reindexer.reindex(movedLayerId)
    }
  }

  it("should reindex a layer") {
    copier.copy(layerId, reindexedLayerId)
    reindexer.reindex(reindexedLayerId)
  }
}
