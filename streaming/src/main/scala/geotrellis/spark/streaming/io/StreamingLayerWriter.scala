package geotrellis.spark.streaming.io

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.merge.Mergable
import geotrellis.util._

import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

trait StreamingLayerWriter[ID] {
  val attributeStore: AttributeStore

  protected def _write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit

  def write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        _write[K, V, M](id, layer, keyIndex)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def write[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        val keyIndex = keyIndexMethod.createIndex(keyBounds)
        _write[K, V, M](id, layer, keyIndex)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def writer[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        StreamingLayerWriter.this.write[K, V, M](id, layer, keyIndexMethod)
    }

  def writer[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]: Mergable
  ](keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        StreamingLayerWriter.this.write[K, V, M](id, layer, keyIndex)
    }
}
