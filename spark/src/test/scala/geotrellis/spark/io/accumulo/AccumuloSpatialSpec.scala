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
          with TestSparkContext
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {
  type Container = RasterRDD[SpatialKey]
  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reader = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData, Container](instance)
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val sample = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, Container](instance, "tiles", RowMajorKeyIndexMethod, SocketWriteStrategy())
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, Container](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, Container](instance, "tiles", HilbertKeyIndexMethod, SocketWriteStrategy())
}


