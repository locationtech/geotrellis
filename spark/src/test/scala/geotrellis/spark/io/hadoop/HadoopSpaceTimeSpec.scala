package geotrellis.spark.io.hadoop

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import org.apache.hadoop.fs.Path
import org.joda.time.DateTime

abstract class HadoopSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile]
          with OnlyIfCanRunSpark
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerCopySpaceTimeTileTests {
  type Container = RasterRDD[SpaceTimeKey]

  lazy val reader = HadoopLayerReader[SpaceTimeKey, Tile, RasterRDD](outputLocal)
  lazy val deleter = HadoopLayerDeleter[SpaceTimeKey](outputLocal)
  lazy val tiles = HadoopTileReader[SpaceTimeKey, Tile](outputLocal)
  lazy val sample =  CoordinateSpaceTime
}

class HadoopSpaceTimeZCurveByYearSpec extends HadoopSpaceTimeSpec {
  lazy val writer = HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](outputLocal, ZCurveKeyIndexMethod.byYear)
  lazy val copier = HadoopLayerCopier(outputLocal)
}

class HadoopSpaceTimeZCurveByFuncSpec extends HadoopSpaceTimeSpec {
  lazy val writer = HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](outputLocal, ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 })
  lazy val copier = HadoopLayerCopier(outputLocal)
}

class HadoopSpaceTimeHilbertSpec extends HadoopSpaceTimeSpec {
  lazy val writer = HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](outputLocal, HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4))
  lazy val copier = HadoopLayerCopier(outputLocal)
}

class HadoopSpaceTimeHilbertWithResolutionSpec extends HadoopSpaceTimeSpec {
  lazy val writer = HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](outputLocal,  HilbertKeyIndexMethod(2))
  lazy val copier = HadoopLayerCopier(outputLocal)
}
