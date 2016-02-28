package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import com.github.nscala_time.time.Imports._

class AccumuloSpaceTimeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData]
    with SpaceTimeKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with CoordinateSpaceTimeTests
    with LayerUpdateSpaceTimeTileTests {
  implicit val instance = MockAccumuloInstance()

  lazy val reader    = AccumuloLayerReader[SpaceTimeKey, Tile, RasterMetaData](instance)
  lazy val writer = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val updater   = AccumuloLayerUpdater[SpaceTimeKey, Tile, RasterMetaData](instance, SocketWriteStrategy())
  lazy val deleter   = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer[SpaceTimeKey, Tile, RasterMetaData](instance, "tiles", ZCurveKeyIndexMethod.byPattern("YMM"), SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[SpaceTimeKey, Tile](instance)
  lazy val sample    =  CoordinateSpaceTime

  lazy val copier = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterMetaData](instance, reader, writer)
  lazy val mover  = GenericLayerMover(copier, deleter)
}
