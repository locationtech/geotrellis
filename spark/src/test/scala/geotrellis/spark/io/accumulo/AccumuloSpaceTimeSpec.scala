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
          with TestSparkContext
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerUpdateSpaceTimeTileTests {
  type Container = RasterRDD[SpaceTimeKey]

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reader  = AccumuloLayerReader[SpaceTimeKey, Tile, RasterRDD](instance)
  lazy val updater = AccumuloLayerUpdater[SpaceTimeKey, Tile, RasterRDD](instance, SocketWriteStrategy())
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val tiles   = AccumuloTileReader[SpaceTimeKey, Tile](instance)
  lazy val sample  =  CoordinateSpaceTime
}

class AccumuloSpaceTimeZCurveByYearSpec extends AccumuloSpaceTimeSpec {
  lazy val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod.byYear, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterRDD](instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(instance, copier, deleter)
}

class AccumuloSpaceTimeZCurveByFuncSpec extends AccumuloSpaceTimeSpec {
  lazy val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", ZCurveKeyIndexMethod.by{ x =>  if (x < DateTime.now) 1 else 0 }, SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterRDD](instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(instance, copier, deleter)
}

class AccumuloSpaceTimeHilbertSpec extends AccumuloSpaceTimeSpec {
  lazy val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles", HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4), SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterRDD](instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(instance, copier, deleter)
}

class AccumuloSpaceTimeHilbertWithResolutionSpec extends AccumuloSpaceTimeSpec {
  lazy val writer = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](instance, "tiles",  HilbertKeyIndexMethod(2), SocketWriteStrategy())
  lazy val copier = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterRDD](instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(instance, copier, deleter)
}
