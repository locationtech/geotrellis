package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.spark.rdd._
import spray.json._

import scala.reflect.ClassTag

trait LayerWriter[ID] {
  val attributeStore: AttributeStore[JsonFormat]

  protected def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K], keyBounds: KeyBounds[K]): Unit

  protected def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K], keyBounds: KeyBounds[K]): Unit = {
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    write[K, V, M](id, layer, keyIndex, keyBounds)
  }

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        write[K, V, M](id, layer, keyIndex, keyBounds)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit =
    layer.metadata.getComponent[Bounds[K]] match {
      case keyBounds: KeyBounds[K] =>
        val keyIndex = keyIndexMethod.createIndex(keyBounds)
        write[K, V, M](id, layer, keyIndex, keyBounds)
      case EmptyBounds =>
        throw new EmptyBoundsError("Cannot write layer with empty bounds.")
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyBounds: KeyBounds[K], keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod, keyBounds)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyBounds: KeyBounds[K], keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndex, keyBounds)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndex)
    }
}
