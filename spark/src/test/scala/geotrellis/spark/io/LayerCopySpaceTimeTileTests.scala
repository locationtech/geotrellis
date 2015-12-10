package geotrellis.spark.io

import geotrellis.raster.Tile
import geotrellis.spark._
import spray.json.JsonFormat

trait LayerCopySpaceTimeTileTests { self: PersistenceSpec[SpaceTimeKey, Tile] with OnlyIfCanRunSpark =>

  def copier: LayerCopier[LayerId]
  lazy val copiedLayerId = layerId.copy(name = s"${layerId.name}-copy")

  if (canRunSpark) {
    it ("shouldn't copy a layer which already exists") {
      intercept[LayerExistsError] {
        copier.copy(layerId, layerId)
      }
    }

    it ("shouldn't copy a layer which doesn't exists)") {
      intercept[LayerNotFoundError] {
        copier.copy(copiedLayerId, copiedLayerId)
      }
    }

    it("should copy a layer") {
      copier.copy(layerId, copiedLayerId)

      reader.read(copiedLayerId).keys.collect().map(println)

      reader.read(copiedLayerId).keys.collect() should contain theSameElementsAs reader.read(layerId).keys.collect()
    }
  }
}
