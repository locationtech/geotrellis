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

class AccumuloLayerUpdater(
  val instance: AccumuloInstance,
  val attributeStore: AttributeStore[JsonFormat],
  options: Options
) extends LayerUpdater[LayerId] {

  protected def _update[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyBounds: KeyBounds[K]) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    implicit val sc = rdd.sparkContext

    val (header, _, keyIndex, _) = try {
      attributeStore.readLayerAttributes[AccumuloLayerHeader, M, KeyIndex[K], Schema](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerUpdateError(id).initCause(e)
    }

    val table = header.tileTable

    if (!(keyIndex.keyBounds contains keyBounds))
      throw new LayerOutOfKeyBoundsError(id, keyIndex.keyBounds)

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
