package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

import com.github.nscala_time.time.Imports._

class FileGridTimeKeySpec
    extends PersistenceSpec[GridTimeKey, Tile, RasterMetadata[GridTimeKey]]
    with GridTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
//    with CoordinateGridTimeKeyTests
    with LayerUpdateGridTimeKeyTileTests {
  lazy val reader = FileLayerReader(outputLocalPath)
  lazy val writer = FileLayerWriter(outputLocalPath)
  lazy val deleter = FileLayerDeleter(outputLocalPath)
  lazy val copier = FileLayerCopier(outputLocalPath)
  lazy val mover  = FileLayerMover(outputLocalPath)
  lazy val reindexer = FileLayerReindexer(outputLocalPath)
  lazy val updater = FileLayerUpdater(outputLocalPath)
  lazy val tiles = FileTileReader[GridTimeKey, Tile](outputLocalPath)
  lazy val sample =  CoordinateGridTimeKey
}
