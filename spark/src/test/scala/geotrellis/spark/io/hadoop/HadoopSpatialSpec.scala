package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark.{OnlyIfCanRunSpark, RasterRDD, SpatialKey, TestEnvironment}

abstract class HadoopSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile]
          with OnlyIfCanRunSpark
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {
  type Container = RasterRDD[SpatialKey]
  val reader = HadoopLayerReader[SpatialKey, Tile, RasterRDD](outputLocal)
  val tiles = HadoopTileReader[SpatialKey, Tile](outputLocal)
  val sample = AllOnesTestFile
}

class HadoopSpatialRowMajorSpec extends HadoopSpatialSpec {
  val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, RowMajorKeyIndexMethod)
}

class HadoopSpatialZCurveSpec extends HadoopSpatialSpec {
  val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, ZCurveKeyIndexMethod)
}

class HadoopSpatialHilbertSpec extends HadoopSpatialSpec {
  val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, HilbertKeyIndexMethod)
}


