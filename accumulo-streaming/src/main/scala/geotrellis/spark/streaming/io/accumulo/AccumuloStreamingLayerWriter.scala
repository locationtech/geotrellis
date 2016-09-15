package geotrellis.spark.streaming.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index._
import geotrellis.spark.merge.Mergable
import geotrellis.spark.streaming.io.StreamingLayerWriter
import geotrellis.util._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import spray.json._

import scala.reflect._

class AccumuloStreamingLayerWriter(
  val attributeStore: AttributeStore,
  instance: AccumuloInstance,
  table: String,
  options: AccumuloLayerWriter.Options
) extends StreamingLayerWriter[LayerId] {

  private def _layerUpdater(implicit sc: SparkContext): LayerUpdater[LayerId] =
    new AccumuloLayerUpdater(instance, attributeStore, new AccumuloLayerReader(attributeStore)(sc, instance), options)

  private def _layerWriter: LayerWriter[LayerId] =
    new AccumuloLayerWriter(attributeStore, instance, table, options)

  protected def _write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) _layerWriter.write[K, V, M](id, rdd, keyIndex)
    else _layerUpdater(rdd.sparkContext).update[K, V, M](id, rdd)
  }
}

object AccumuloStreamingLayerWriter {
  def apply(
    instance: AccumuloInstance,
    table: String,
    options: AccumuloLayerWriter.Options
  ): AccumuloStreamingLayerWriter =
    new AccumuloStreamingLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  ): AccumuloStreamingLayerWriter =
    new AccumuloStreamingLayerWriter(
      attributeStore = AccumuloAttributeStore(instance.connector),
      instance = instance,
      table = table,
      options = AccumuloLayerWriter.Options.DEFAULT
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String,
    options: AccumuloLayerWriter.Options
  ): AccumuloStreamingLayerWriter =
    new AccumuloStreamingLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = options
    )

  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    table: String
  ): AccumuloStreamingLayerWriter =
    new AccumuloStreamingLayerWriter(
      attributeStore = attributeStore,
      instance = instance,
      table = table,
      options = AccumuloLayerWriter.Options.DEFAULT
    )
}
