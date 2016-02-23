package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reader = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData](instance)
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetaData](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val sample = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData](instance, "tiles", RowMajorKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData](instance, reader, writer)
  lazy val mover  = GenericLayerMover(copier, deleter)
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData](instance, reader, writer)
  lazy val mover  = GenericLayerMover(copier, deleter)
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData](instance, "tiles", HilbertKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData](instance, reader, writer)
  lazy val mover  = GenericLayerMover(copier, deleter)
}


