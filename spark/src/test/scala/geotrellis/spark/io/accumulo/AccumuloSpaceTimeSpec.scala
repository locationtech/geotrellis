package geotrellis.spark.io.accumulo

import com.github.nscala_time.time.Imports._
import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import org.joda.time.DateTime

abstract class AccumuloSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetadata]
    with TestEnvironment
    with TestFiles
    with CoordinateSpaceTimeTests
    with LayerUpdateSpaceTimeTileTests {
  override val layerId  = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reindexerKeyIndexMethod = ZCurveKeyIndexMethod.byMonth

  lazy val reader    = AccumuloLayerReader[SpaceTimeKey, Tile, RasterMetadata](instance)
  lazy val updater   = AccumuloLayerUpdater[SpaceTimeKey, Tile, RasterMetadata](instance, SocketWriteStrategy())
  lazy val deleter   = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer[SpaceTimeKey, Tile, RasterMetadata](instance, "tiles", SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpaceTimeKey, Tile](instance)
  lazy val writer    = AccumuloLayerWriter[SpaceTimeKey, Tile, RasterMetadata](instance, "tiles",SocketWriteStrategy())
  lazy val copier    = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterMetadata](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
  lazy val sample    = CoordinateSpaceTime
}

class AccumuloSpaceTimeZCurveByYearSpec extends AccumuloSpaceTimeSpec {
  lazy val writerKeyIndexMethod = ZCurveKeyIndexMethod.byYear
}

/*class AccumuloSpaceTimeZCurveByFuncSpec extends AccumuloSpaceTimeSpec {
  lazy val writerKeyIndexMethod = ZCurveKeyIndexMethod.by({ x =>  if (x < DateTime.now) 1 else 0 }, "AccumuloSpaceTimeZCurveByFuncSpec")
}*/

class AccumuloSpaceTimeHilbertSpec extends AccumuloSpaceTimeSpec {
  lazy val writerKeyIndexMethod = HilbertKeyIndexMethod(DateTime.now - 20.years, DateTime.now, 4)
}

class AccumuloSpaceTimeHilbertWithResolutionSpec extends AccumuloSpaceTimeSpec {
  lazy val writerKeyIndexMethod = HilbertKeyIndexMethod(2)
}
