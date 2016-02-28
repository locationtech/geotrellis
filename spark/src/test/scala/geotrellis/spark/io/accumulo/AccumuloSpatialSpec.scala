package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._

class AccumuloSpatialSpec
  extends PersistenceSpec[SpatialKey, Tile, RasterMetaData]
    with SpatialKeyIndexMethods
    with TestEnvironment
    with TestFiles
    with AllOnesTestTileTests {

  implicit val instance = MockAccumuloInstance()

  lazy val reader = AccumuloLayerReader[SpatialKey, Tile, RasterMetaData](instance)
  lazy val writer = AccumuloLayerWriter(instance, "tiles", SocketWriteStrategy())
  lazy val deleter = AccumuloLayerDeleter(instance)
  lazy val reindexer = AccumuloLayerReindexer[SpatialKey, Tile, RasterMetaData](instance, "tiles", ZCurveKeyIndexMethod, SocketWriteStrategy())
  lazy val tiles = AccumuloTileReader[SpatialKey, Tile](instance)
  lazy val sample = AllOnesTestFile

  lazy val copier = AccumuloLayerCopier[SpatialKey, Tile, RasterMetaData](instance, reader, writer)
  lazy val mover  = GenericLayerMover(copier, deleter)
}
