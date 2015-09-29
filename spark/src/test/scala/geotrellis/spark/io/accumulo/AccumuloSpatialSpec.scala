package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

abstract class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile]
          with OnlyIfCanRunSpark
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {
  type Container = RasterRDD[SpatialKey]
  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  val reader = AccumuloLayerReader[SpatialKey, Tile, RasterRDD](instance)
  val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  val sample = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", RowMajorKeyIndexMethod, SocketWriteStrategy())
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", HilbertKeyIndexMethod, SocketWriteStrategy())
}


