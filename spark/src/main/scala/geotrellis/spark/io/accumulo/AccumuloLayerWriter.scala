package geotrellis.spark.io.accumulo

import geotrellis.spark.io.json._
import geotrellis.spark.io.avro._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class AccumuloLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, V],
    keyIndexMethod: KeyIndexMethod[K],
    table: String)
  (implicit val cons: ContainerConstructor[K, V, Container])
  extends UpdatingLayerWriter[LayerId, K, V, Container with RDD[(K, V)]] {

  def write(id: LayerId, rdd: Container with RDD[(K, V)]): Unit = {
    try {
      val header =
        AccumuloLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          tileTable = table
        )
      val metaData = cons.getMetaData(rdd)
      val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
      val keyIndex = keyIndexMethod.createIndex(keyBounds)

      implicit val mdFormat = cons.metaDataFormat
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, rddWriter.schema)

      val getRowId = (key: K) => index2RowId(keyIndex.toIndex(key))

      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }

  def update(id: LayerId, rdd: RDD[(K, V)]) = {
    try {
      if (!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)
      implicit val mdFormat = cons.metaDataFormat
      val header =
        AccumuloLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          tileTable = table
        )

      val (existingHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
        attributeStore.readLayerAttributes[AccumuloLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      if (existingHeader != header) throw new HeaderMatchError(id, existingHeader, header)

      val boundable = implicitly[Boundable[K]]
      val keyBounds = boundable.getKeyBounds(rdd)

      if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
        throw new OutOfKeyBoundsError(id)

      val getRowId = (key: K) => index2RowId(existingKeyIndex.toIndex(key))

      rddWriter.write(rdd, table, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object AccumuloLayerWriter {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
            V: AvroRecordCodec: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   table: String,
   indexMethod: KeyIndexMethod[K],
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit cons: ContainerConstructor[K, V, Container[K]]): AccumuloLayerWriter[K, V, Container[K]] =
    new AccumuloLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy),
      keyIndexMethod = indexMethod,
      table = table
    )
}