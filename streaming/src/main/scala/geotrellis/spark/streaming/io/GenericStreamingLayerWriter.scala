package geotrellis.spark.streaming.io

import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.{Boundable, Bounds, LayerId, Metadata}
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.{AttributeStore, LayerUpdater, LayerWriter}
import geotrellis.spark.merge.Mergable
import geotrellis.util.GetComponent

import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class GenericStreamingLayerWriter(val attributeStore: AttributeStore, val layerWriter: LayerWriter[LayerId], val layerUpdater: LayerUpdater[LayerId]) extends StreamingLayerWriter[LayerId] {
  protected def _write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) layerWriter.write[K, V, M](id, rdd, keyIndex)
    else layerUpdater.update[K, V, M](id, rdd)
  }
}

object GenericStreamingLayerWriter {
  def apply(attributeStore: AttributeStore, layerWriter: LayerWriter[LayerId], layerUpdater: LayerUpdater[LayerId]): StreamingLayerWriter[LayerId] =
    new GenericStreamingLayerWriter(attributeStore, layerWriter, layerUpdater)
}