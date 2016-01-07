package geotrellis.spark.io.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index.hilbert.HilbertSpatialKeyIndex
import geotrellis.spark.io.index.rowmajor.RowMajorSpatialKeyIndex
import geotrellis.spark.io.index.zcurve.ZSpatialKeyIndex
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._

abstract class HadoopSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with AllOnesTestTileTests {

  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val sample = AllOnesTestFile
}

class HadoopSpatialRowMajorSpec extends HadoopSpatialSpec {
  lazy val reader    = HadoopLayerReader[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](outputLocal)
  lazy val copier    = HadoopLayerCopier[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](outputLocal)
  lazy val mover     = HadoopLayerMover[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](outputLocal)
  lazy val reindexer = HadoopLayerReindexer[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex, ZSpatialKeyIndex](outputLocal, ZCurveKeyIndexMethod)
  lazy val tiles     = HadoopTileReader[SpatialKey, Tile, RowMajorSpatialKeyIndex](outputLocal)
  lazy val writer    = HadoopLayerWriter[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex](outputLocal, RowMajorKeyIndexMethod)
}

class HadoopSpatialZCurveSpec extends HadoopSpatialSpec {
  lazy val reader    = HadoopLayerReader[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](outputLocal)
  lazy val copier    = HadoopLayerCopier[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](outputLocal)
  lazy val mover     = HadoopLayerMover[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](outputLocal)
  lazy val reindexer = HadoopLayerReindexer[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex, ZSpatialKeyIndex](outputLocal, ZCurveKeyIndexMethod)
  lazy val tiles     = HadoopTileReader[SpatialKey, Tile, ZSpatialKeyIndex](outputLocal)
  lazy val writer    = HadoopLayerWriter[SpatialKey, Tile, RasterMetaData, ZSpatialKeyIndex](outputLocal, ZCurveKeyIndexMethod)
}

class HadoopSpatialHilbertSpec extends HadoopSpatialSpec {
  lazy val reader    = HadoopLayerReader[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](outputLocal)
  lazy val copier    = HadoopLayerCopier[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](outputLocal)
  lazy val mover     = HadoopLayerMover[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](outputLocal)
  lazy val reindexer = HadoopLayerReindexer[SpatialKey, Tile, RasterMetaData, RowMajorSpatialKeyIndex, ZSpatialKeyIndex](outputLocal, ZCurveKeyIndexMethod)
  lazy val tiles     = HadoopTileReader[SpatialKey, Tile, ZSpatialKeyIndex](outputLocal)
  lazy val writer    = HadoopLayerWriter[SpatialKey, Tile, RasterMetaData, HilbertSpatialKeyIndex](outputLocal, HilbertKeyIndexMethod)
}


