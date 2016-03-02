package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData[SpatialKey]]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with AccumuloTestEnvironment
    with TestFiles
    with AllOnesTestTileTests {

  implicit val instance = MockAccumuloInstance()

  lazy val reader = AccumuloLayerReader(instance)
  lazy val writer = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer(instance, SocketWriteStrategy())
  lazy val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val sample = AllOnesTestFile

  lazy val copier = AccumuloLayerCopier(instance, reader, writer)
  lazy val mover  = AccumuloLayerMover(copier, deleter)
}
