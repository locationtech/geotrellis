package geotrellis.spark.io.accumulo

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import org.joda.time.DateTime

abstract class AccumuloSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile]
          with OnlyIfCanRunSpark
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests {
  type Container = RasterRDD[SpaceTimeKey]

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  val reader = AccumuloLayerReader[SpaceTimeKey, Tile, RasterRDD](instance)
  val tiles = AccumuloTileReader[SpaceTimeKey, Tile](instance)
  val sample =  CoordinateSpaceTime
}

class AccumuloSpaceTimeZCurveByYearSpec extends AccumuloSpaceTimeSpec {
  val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod.byYear, SocketWriteStrategy())
}

class AccumuloSpaceTimeZCurveByFuncSpec extends AccumuloSpaceTimeSpec {
  val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 }, SocketWriteStrategy())
}

class AccumuloSpaceTimeHilbertSpec extends AccumuloSpaceTimeSpec {
  val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4), SocketWriteStrategy())
}

class AccumuloSpaceTimeHilbertWithResolutionSpec extends AccumuloSpaceTimeSpec {
  val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles",  HilbertKeyIndexMethod(2), SocketWriteStrategy())
}
