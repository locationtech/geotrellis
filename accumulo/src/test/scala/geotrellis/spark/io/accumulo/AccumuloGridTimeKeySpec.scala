package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

class AccumuloGridTimeKeySpec
  extends PersistenceSpec[GridTimeKey, Tile, RasterMetadata[GridTimeKey]]
    with GridTimeKeyIndexMethods
    with TestEnvironment
    with AccumuloTestEnvironment
    with TestFiles
    with CoordinateGridTimeKeyTests
    with LayerUpdateGridTimeKeyTileTests {
  implicit lazy val instance = MockAccumuloInstance()

  lazy val reader    = AccumuloLayerReader(instance)
  lazy val writer = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val deleter   = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer(instance, SocketWriteStrategy())
  lazy val updater   = AccumuloLayerUpdater(instance, SocketWriteStrategy())
  lazy val tiles     = AccumuloTileReader[GridTimeKey, Tile](instance)
  lazy val sample    = CoordinateGridTimeKey
  lazy val copier = AccumuloLayerCopier(instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(copier, deleter)
}
