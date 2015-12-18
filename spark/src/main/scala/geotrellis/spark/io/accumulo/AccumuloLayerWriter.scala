package geotrellis.spark.io.accumulo

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io._
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

  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
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
    apply[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](instance, table, keyIndexMethod, strategy)

  def spatialMultiBand(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpatialKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpatialKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpatialKey]]) =
    apply[SpatialKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpatialKey]](instance, table, keyIndexMethod, strategy)

  def spaceTime(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, Tile)], RasterMetaData), RasterRDD[SpaceTimeKey]]) =
    apply[SpaceTimeKey, Tile, RasterMetaData, RasterRDD[SpaceTimeKey]](instance, table, keyIndexMethod, strategy)

  def spaceTimeMultiBand(instance: AccumuloInstance, table: String, keyIndexMethod: KeyIndexMethod[SpaceTimeKey], strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit sc: SparkContext, bridge: Bridge[(RDD[(SpaceTimeKey, MultiBandTile)], RasterMetaData), MultiBandRasterRDD[SpaceTimeKey]]) =
    apply[SpaceTimeKey, MultiBandTile, RasterMetaData, MultiBandRasterRDD[SpaceTimeKey]](instance, table, keyIndexMethod, strategy)
}
