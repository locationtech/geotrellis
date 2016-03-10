package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles

class AccumuloGridKeySpec
  extends PersistenceSpec[GridKey, Tile, RasterMetadata[GridKey]]
    with GridKeyIndexMethods
    with TestEnvironment
    with AccumuloTestEnvironment
    with TestFiles
    with AllOnesTestTileTests {

  implicit lazy val instance = MockAccumuloInstance()

  lazy val reader = AccumuloLayerReader(instance)
  lazy val writer = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer(instance, SocketWriteStrategy())
  lazy val updater   = AccumuloLayerUpdater(instance, SocketWriteStrategy())
  lazy val tiles = AccumuloTileReader[GridKey, Tile](instance)
  lazy val sample = AllOnesTestFile

  lazy val copier = AccumuloLayerCopier(instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(copier, deleter)
}
