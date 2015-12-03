package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

class AccumuloLayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, V])
  (implicit val cons: ContainerConstructor[K, V, Container])
  extends LayerUpdater[LayerId, K, V, Container with RDD[(K, V)]] {

  def update(id: LayerId, rdd: Container with RDD[(K, V)]) = {
    try {
      if (!attributeStore.layerExists(id)) throw new LayerNotExistsError(id)
      implicit val sc = rdd.sparkContext
      implicit val mdFormat = cons.metaDataFormat

      val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) =
        attributeStore.readLayerAttributes[AccumuloLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      val boundable = implicitly[Boundable[K]]
      val keyBounds = boundable.getKeyBounds(rdd)

      if ((existingKeyBounds includes keyBounds.minKey) || !(existingKeyBounds includes keyBounds.maxKey))
        throw new OutOfKeyBoundsError(id)

      val getRowId = (key: K) => index2RowId(existingKeyIndex.toIndex(key))

      rddWriter.write(rdd, existingHeader.tileTable, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object AccumuloLayerUpdater {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag, Container[_]]
  (instance: AccumuloInstance,
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit cons: ContainerConstructor[K, V, Container[K]]): AccumuloLayerUpdater[K, V, Container[K]] =
    new AccumuloLayerUpdater(
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy)
    )
}
