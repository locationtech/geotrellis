package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark._

trait LayerMoveSpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile] with OnlyIfCanRunSpark =>

  def mover: LayerMover[LayerId]
  lazy val movedLayerId = layerId.copy(name = s"${layerId.name}-move")

  if (canRunSpark) {
    it ("shouldn't move a layer which already exists") {
      intercept[LayerExistsError] {
        mover.move(layerId, layerId)
      }
    }

    it ("shouldn't move a layer which doesn't exists)") {
      intercept[LayerNotFoundError] {
        mover.move(movedLayerId, movedLayerId)
      }
    }

    it("should move a layer") {
      val keysBeforeMove = reader.read(layerId).keys.collect()
      mover.move(layerId, movedLayerId)
      intercept[LayerNotFoundError] {
        reader.read(layerId)
      }
      keysBeforeMove should contain theSameElementsAs reader.read(movedLayerId).keys.collect()
      mover.move(movedLayerId, layerId) // move back
    }
  }
}
