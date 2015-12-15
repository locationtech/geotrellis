package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._

abstract class HadoopSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {
  type Container = RasterRDD[SpatialKey]
  lazy val reader = HadoopLayerReader[SpatialKey, Tile, RasterRDD](outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier[SpatialKey, Tile, RasterRDD](outputLocal)
  lazy val mover  = HadoopLayerMover[SpatialKey, Tile, RasterRDD](outputLocal)
  lazy val reindexer = HadoopLayerReindexer[SpatialKey, Tile, RasterRDD](outputLocal, ZCurveKeyIndexMethod)
  lazy val tiles = HadoopTileReader[SpatialKey, Tile](outputLocal)
  lazy val sample = AllOnesTestFile
}

class HadoopSpatialRowMajorSpec extends HadoopSpatialSpec {
  lazy val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, RowMajorKeyIndexMethod)
}

class HadoopSpatialZCurveSpec extends HadoopSpatialSpec {
  lazy val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, ZCurveKeyIndexMethod)
}

class HadoopSpatialHilbertSpec extends HadoopSpatialSpec {
  lazy val writer = HadoopLayerWriter[SpatialKey, Tile, RasterRDD](outputLocal, HilbertKeyIndexMethod)
}


