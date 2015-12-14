package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark.io.index.{ZCurveKeyIndexMethod, KeyIndexMethod}
import geotrellis.spark.{LayerId, OnlyIfCanRunSpark, SpaceTimeKey, SpatialKey}

trait LayerReindexSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile] with OnlyIfCanRunSpark =>

  def reindexer: TestReindexer
  lazy val reindexedLayerId  = layerId.copy(name = s"${layerId.name}-reindexed")

  if (canRunSpark) {
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
}
