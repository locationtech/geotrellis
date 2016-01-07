package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex
import geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val sample = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  lazy val reader    = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex, ZSpatialKeyIndex](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpatialKey, Tile, RowMajorSpatialKeyIndex](instance)
  lazy val writer    = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](instance, "tiles", RowMajorKeyIndexMethod, SocketWriteStrategy())
  lazy val copier    = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  lazy val reader    = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpatialKey, Tile, ZSpatialKeyIndex](instance)
  lazy val writer    = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val copier    = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  lazy val reader    = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex, ZSpatialKeyIndex](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpatialKey, Tile, HilbertSpatialKeyIndex](instance)
  lazy val writer    = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](instance, "tiles", HilbertKeyIndexMethod, SocketWriteStrategy())
  lazy val copier    = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
}


