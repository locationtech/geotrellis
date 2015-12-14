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

  lazy val reader = AccumuloLayerReader[SpatialKey, Tile, RasterRDD](instance)
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val sample = AllOnesTestFile
}

class AccumuloSpatialRowMajorSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", RowMajorKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterRDD](instance, reader, writer)
}

class AccumuloSpatialZCurveSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterRDD](instance, reader, writer)
}

class AccumuloSpatialHilbertSpec extends AccumuloSpatialSpec {
  lazy val writer = AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](instance, "tiles", HilbertKeyIndexMethod, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterRDD](instance, reader, writer)
}


