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
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {
  lazy val reindexerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod

  lazy val reader    = FileLayerReader[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val deleter   = FileLayerDeleter[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val copier    = FileLayerCopier[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val mover     = FileLayerMover[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val reindexer = FileLayerReindexer[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val tiles     = FileTileReader[SpatialKey, Tile](outputLocalPath)
  lazy val writer    = FileLayerWriter[SpatialKey, Tile, RasterMetaData](outputLocalPath)
  lazy val sample    = AllOnesTestFile
}

class FileSpatialRowMajorSpec extends FileSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = RowMajorKeyIndexMethod
}

class FileSpatialZCurveSpec extends FileSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod
}

class FileSpatialHilbertSpec extends FileSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = HilbertKeyIndexMethod
}
