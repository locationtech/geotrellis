package geotrellis.spark.io.accumulo

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.{LayerWriteError, AttributeStore, ContainerConstructor, Writer}
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

class AccumuloLayerWriter[K: Boundable: JsonFormat: ClassTag, TileType: ClassTag, Container](
    val attributeStore: AttributeStore.Aux[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, TileType],
    keyIndexMethod: KeyIndexMethod[K],
    table: String)
  (implicit val cons: ContainerConstructor[K, TileType, Container])
  extends Writer[LayerId, Container with RDD[(K, TileType)]] {

  def write(id: LayerId, rdd: Container with RDD[(K, TileType)]): Unit = {
    try {
      val layerMetaData =
        AccumuloLayerMetaData(
          keyClass = classTag[K].toString(),
          valueClass = classTag[TileType].toString(),
          tileTable = table
        )
      val rasterMetaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, TileType)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      attributeStore.cacheWrite(id, Fields.layerMetaData, layerMetaData)
      attributeStore.cacheWrite(id, Fields.rddMetadata, rasterMetaData)(cons.metaDataFormat)
      attributeStore.cacheWrite(id, Fields.keyBounds, keyBounds)
      attributeStore.cacheWrite(id, Fields.keyIndex, keyIndex)
      attributeStore.cacheWrite(id, Fields.schema, rddWriter.schema.toString.parseJson)

      val getRowId = (key: K) => index2RowId(keyIndex.toIndex(key))

      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, C[_]](
      instance: AccumuloInstance,
      table: String,
      indexMethod: KeyIndexMethod[K],
      strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
    (implicit cons: ContainerConstructor[K, V, C[K]]): AccumuloLayerWriter[K, V, C[K]] =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = indexMethod,
      table = table
    )
}