package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{BoundedKeyIndex, KeyIndex}
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import spray.json._
import scala.reflect._

class AccumuloLayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: BaseAccumuloRDDWriter[K, V])
  extends LayerUpdater[LayerId, K, V, M] {

  def update[I <: BoundedKeyIndex[K]: JsonFormat](id: LayerId, rdd: Container, format: JsonFormat[I]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext

    val (existingHeader, existingMetadata, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], I, Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val keyIndexSpace = existingKeyIndex.keyBounds
    val boundable = implicitly[Boundable[K]]
    val keyBounds = try {
      boundable.getKeyBounds(rdd)
    } catch {
      case e: UnsupportedOperationException => throw new LayerUpdateError(id, ": empty rdd update").initCause(e)
    }

    if (!boundable.includes(keyBounds.minKey, keyIndexSpace) || !boundable.includes(keyBounds.maxKey, keyIndexSpace))
      throw new LayerOutOfKeyBoundsError(id)

    val getRowId = (key: K) => index2RowId(existingKeyIndex.toIndex(key))

    try {
      attributeStore.writeLayerAttributes(
        id, existingHeader, existingMetadata, boundable.combine(existingKeyBounds, keyBounds), existingKeyIndex, rddWriter.schema
      )
      rddWriter.write(rdd, existingHeader.tileTable, columnFamily(id), getRowId, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object AccumuloLayerUpdater {
  def apply[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    instance: AccumuloInstance,
    strategy: AccumuloWriteStrategy = AccumuloLayerWriter.defaultAccumuloWriteStrategy
  ): AccumuloLayerUpdater[K, V, M] =
    new AccumuloLayerUpdater[K, V, M](
      attributeStore = AccumuloAttributeStore(instance.connector),
      rddWriter = new AccumuloRDDWriter[K, V](instance, strategy)
    )
}
