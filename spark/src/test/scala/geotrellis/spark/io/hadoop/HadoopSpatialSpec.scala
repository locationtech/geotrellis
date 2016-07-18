package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles


class HadoopSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileSpec {

  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val updater = HadoopLayerUpdater(outputLocal)
  lazy val tiles = HadoopValueReader(outputLocal)
  lazy val sample = AllOnesTestFile

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some layer", 10)

      writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some:layer", 10)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      }
    }
  }
}
