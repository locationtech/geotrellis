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

class AccumuloLayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: BaseAccumuloRDDWriter[K, V])
  (implicit val cons: Bridge[(RDD[(K, V)], M), C])
  extends LayerUpdater[LayerId, K, V, M, C] {

  def update(id: LayerId, rdd: C) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext

    val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val boundable = implicitly[Boundable[K]]
    val keyBounds = try {
      boundable.getKeyBounds(rdd)
    } catch {
      case e: UnsupportedOperationException => throw new LayerUpdateError(id, ": empty rdd update").initCause(e)
    }

    if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
      throw new LayerOutOfKeyBoundsError(id)

    val getRowId = (key: K) => index2RowId(existingKeyIndex.toIndex(key))

    try {
      rddWriter.write(rdd, existingHeader.tileTable, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerUpdater {
  def defaultAccumuloWriteStrategy = HdfsWriteStrategy("/geotrellis-ingest")

  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
  V: AvroRecordCodec: ClassTag, M: JsonFormat, C <: RDD[(K, V)]]
  (instance: AccumuloInstance,
   strategy: AccumuloWriteStrategy = defaultAccumuloWriteStrategy)
  (implicit cons: Bridge[(RDD[(K, V)], M), C]): AccumuloLayerUpdater[K, V, M, C] =
    new AccumuloLayerUpdater[K, V, M, C](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy)
    )
}
