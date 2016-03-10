package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

class FileGridKeySpec
    extends PersistenceSpec[GridKey, Tile, RasterMetadata[GridKey]]
    with GridKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {
  lazy val reader = FileLayerReader(outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val updater = FileLayerUpdater(outputLocalPath)
  lazy val tiles = FileTileReader[GridKey, Tile](outputLocalPath)
  lazy val sample = AllOnesTestFile
}
