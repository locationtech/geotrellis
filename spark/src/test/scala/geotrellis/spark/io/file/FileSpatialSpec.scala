package geotrellis.spark.io.file

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._

abstract class FileSpatialSpec
    extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with TestEnvironment with TestFiles
    with AllOnesTestTileTests {
  val catalogPath = outputLocalPath

  lazy val reader = FileLayerReader[SpatialKey, Tile, RasterMetaData](catalogPath)
  lazy val deleter = FileLayerDeleter[SpatialKey, Tile, RasterMetaData](catalogPath)
  lazy val copier = FileLayerCopier[SpatialKey, Tile, RasterMetaData](catalogPath)
  lazy val mover  = FileLayerMover[SpatialKey, Tile, RasterMetaData](catalogPath)
  lazy val reindexer = FileLayerReindexer[SpatialKey, Tile, RasterMetaData](catalogPath, ZCurveKeyIndexMethod)
  lazy val tiles = FileTileReader[SpatialKey, Tile](catalogPath)
  lazy val sample = AllOnesTestFile
}

class FileSpatialRowMajorSpec extends FileSpatialSpec {
  lazy val writer = FileLayerWriter[SpatialKey, Tile, RasterMetaData](catalogPath, RowMajorKeyIndexMethod)
}

class FileSpatialZCurveSpec extends FileSpatialSpec {
  lazy val writer = FileLayerWriter[SpatialKey, Tile, RasterMetaData](catalogPath, ZCurveKeyIndexMethod)
}

class FileSpatialHilbertSpec extends FileSpatialSpec {
  lazy val writer = FileLayerWriter[SpatialKey, Tile, RasterMetaData](catalogPath, HilbertKeyIndexMethod)
}
