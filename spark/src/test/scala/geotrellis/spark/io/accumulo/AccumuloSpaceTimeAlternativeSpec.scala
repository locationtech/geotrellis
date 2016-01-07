package geotrellis.spark.io.accumulo

import geotrellis.raster.Tile
import geotrellis.spark.io.accumulo.spacetime.{SpaceTimeAccumuloRDDReader, SpaceTimeAccumuloRDDWriter}
import geotrellis.spark.io.index.{KeyIndexMethod, ZCurveKeyIndexMethod}
import geotrellis.spark.io.index.zcurve.ZSpaceTimeKeyIndex
import geotrellis.spark.testfiles.TestFiles
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro.codecs._

class AccumuloSpaceTimeAlternativeSpec
  extends PersistenceSpec[SpaceTimeKey, Tile, RasterMetaData]
          with TestSparkContext
          with TestEnvironment with TestFiles
          with CoordinateSpaceTimeTests
          with LayerUpdateSpaceTimeTileTests {

  override val layerId = LayerId(name, 1)
  implicit val instance = MockAccumuloInstance()

  lazy val reader = new AccumuloLayerReader[SpaceTimeKey, Tile, RasterMetaData, ZSpaceTimeKeyIndex] (
    AccumuloAttributeStore(instance.connector),
    new SpaceTimeAccumuloRDDReader[Tile](instance))

  lazy val writer =
    new AccumuloLayerWriter[SpaceTimeKey, Tile, RasterMetaData, ZSpaceTimeKeyIndex](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new SpaceTimeAccumuloRDDWriter[Tile](instance, SocketWriteStrategy()),
      keyIndexMethod = ZCurveKeyIndexMethod.byYear,
      table = "tiles")

  lazy val updater = new AccumuloLayerUpdater[SpaceTimeKey, Tile, RasterMetaData, ZSpaceTimeKeyIndex] (
    AccumuloAttributeStore(instance.connector),
    new SpaceTimeAccumuloRDDWriter[Tile](instance, SocketWriteStrategy()))

  lazy val deleter   = new AccumuloLayerDeleter(AccumuloAttributeStore(instance.connector), instance.connector)
  lazy val copier    = AccumuloLayerCopier[SpaceTimeKey, Tile, RasterMetaData, ZSpaceTimeKeyIndex](instance, reader, writer)
  lazy val mover     = GenericLayerMover(copier, deleter)
  lazy val reindexer = AccumuloLayerReindexer[SpaceTimeKey, Tile, RasterMetaData, ZSpaceTimeKeyIndex](instance, "tiles", ZCurveKeyIndexMethod.byPattern("YMM"), SocketWriteStrategy())

  lazy val tiles  = AccumuloTileReader[SpaceTimeKey, Tile, ZSpaceTimeKeyIndex](instance)
  lazy val sample = CoordinateSpaceTime
}
