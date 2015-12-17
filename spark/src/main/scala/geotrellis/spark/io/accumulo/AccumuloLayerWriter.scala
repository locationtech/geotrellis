package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class AccumuloLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    table: String)
  (implicit bridge: Bridge[(RDD[(K, V)], M), C])
  extends Writer[LayerId, C] {

  def write(id: LayerId, rdd: C): Unit = {
    val header =
      AccumuloLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        tileTable = table
      )
    val (_, metaData) = bridge.unapply(rdd)
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd)
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    val getRowId = (key: K) => index2RowId(keyIndex.toIndex(key))

    try {
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, rddWriter.schema)
      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
            V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit cons: Bridge[(RDD[(K, V)], M), C]): AccumuloLayerWriter[K, V, M, C] =
    new AccumuloLayerWriter[K, V, M, C](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = indexMethod,
      table = table
    )

  def spatial(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpatialKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, Tile)], RasterMetaData), RasterRDD[SpatialKey]]) =
    new AccumuloLayerWriter[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDWriter[SpatialKey, Tile](instance, strategy),
      keyIndexMethod, table)

  def spatialMultiBand(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpatialKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpatialKey]]) =
    new AccumuloLayerWriter[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDWriter[SpatialKey, MultiBandTile](instance, strategy),
      keyIndexMethod, table)

  def spaceTime(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, Tile)], RasterMetaData), RasterRDD[SpaceTimeKey]]) =
    new AccumuloLayerWriter[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDWriter[SpaceTimeKey, Tile](instance, strategy),
      keyIndexMethod, table)

  def spaceTimeMultiBand(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpaceTimeKey]]) =
    new AccumuloLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](
      AccumuloAttributeStore(instance.connector),
      new AccumuloRDDWriter[SpaceTimeKey, MultiBandTile](instance, strategy),
      keyIndexMethod, table)
}