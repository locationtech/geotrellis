package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetadata]
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {
  override val layerId  = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reindexerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod

  lazy val reader    = AccumuloLayerReader[SpatialKey, Tile, RasterMetadata](instance)
  lazy val deleter   = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetadata](instance, "tiles", SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val writer    = AccumuloLayerWriter[SpatialKey, Tile, RasterMetadata](instance, "tiles", SocketWriteStrategy())
  lazy val copier    = AccumuloLayerCopier[SpatialKey, Tile, RasterMetadata](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
  lazy val sample    = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = RowMajorKeyIndexMethod
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = ZCurveKeyIndexMethod
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  lazy val writerKeyIndexMethod: KeyIndexMethod[SpatialKey] = HilbertKeyIndexMethod
}


