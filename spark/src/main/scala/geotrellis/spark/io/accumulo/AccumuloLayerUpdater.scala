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

import AccumuloLayerWriter.Options

// TODO: What happens if the schema changes between initial write and update?
// Check to see if Schema is the same.
// If not, we need to rewrite the whole layer.
class AccumuloLayerUpdater(
  val instance: AccumuloInstance,
  val attributeStore: AttributeStore[JsonFormat],
  options: Options
) extends LayerUpdater[LayerId] {

  // TODO: fix
  def update[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext

    val (header, metaData, existingKeyBounds, keyIndex, _) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val table = header.tileTable

    val boundable = implicitly[Boundable[K]]
    val keyBounds = try {
      boundable.getKeyBounds(rdd)
    } catch {
      case e: UnsupportedOperationException => throw new LayerUpdateError(id, ": empty rdd update").initCause(e)
    }

    if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
      throw new LayerOutOfKeyBoundsError(id)

    val encodeKey = (key: K) => AccumuloKeyEncoder.encode(id, key, keyIndex.toIndex(key))


    try {
      AccumuloRDDWriter.write(rdd, instance, encodeKey, options.writeStrategy, table, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object AccumuloLayerUpdater {
  def apply(instance: AccumuloInstance, options: Options = Options.DEFAULT): AccumuloLayerUpdater =
    new AccumuloLayerUpdater(
      instance = instance,
      attributeStore = AccumuloAttributeStore(instance.connector),
      options = options
    )
}
