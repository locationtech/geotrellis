package geotrellis.spark.io.file

import geotrellis.raster.{Tile, TileFeature}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestTileFeatureFiles

import org.apache.spark.rdd.RDD


class FileTileFeatureSpatialSpec
    extends PersistenceSpec[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestTileFeatureFiles
    with AllOnesTestTileFeatureSpec {
  lazy val reader = FileLayerReader(outputLocalPath)
  lazy val creader = FileLayerCollectionReader(outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val updater = FileLayerUpdater(outputLocalPath)
  lazy val tiles = FileValueReader(outputLocalPath)
  lazy val sample = AllOnesTestFile

  describe("Filesystem layer names") {
    it("should not throw with bad characters in name") {
      val layer = AllOnesTestFile
      val layerId = LayerId("Some!layer:%@~`{}id", 10)

      println(outputLocalPath)
      writer.write[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, TileFeature[Tile, Tile], TileLayerMetadata[SpatialKey]](layerId)
    }
  }
}
