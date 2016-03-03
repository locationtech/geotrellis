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

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K], keyBounds: KeyBounds[K]): Unit

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K], keyBounds: KeyBounds[K]): Unit = {
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    write[K, V, M](id, layer, keyIndex, keyBounds)
  }

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Component[?, KeyBounds[K]]: JsonFormat
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val keyBounds = layer.metadata.getComponent[KeyBounds[K]]
    write[K, V, M](id, layer, keyIndex, keyBounds)
  }

  def write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Component[?, KeyBounds[K]]: JsonFormat
  ](id: ID, layer: RDD[(K, V)] with Metadata[M], keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val keyBounds = layer.metadata.getComponent[KeyBounds[K]]
    val keyIndex = keyIndexMethod.createIndex(keyBounds)
    write[K, V, M](id, layer, keyIndex, keyBounds)
  }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](keyBounds: KeyBounds[K], keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod, keyBounds)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](keyBounds: KeyBounds[K], keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndex, keyBounds)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Component[?, KeyBounds[K]]: JsonFormat
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod)
    }

  def writer[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: Component[?, KeyBounds[K]]: JsonFormat
  ](keyIndex: KeyIndex[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) =
        LayerWriter.this.write[K, V, M](id, layer, keyIndex)
    }

  def keyBoundsComputingWriter[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](keyIndexMethod: KeyIndexMethod[K]):  Writer[ID, RDD[(K, V)] with Metadata[M]] =
    new Writer[ID, RDD[(K, V)] with Metadata[M]] {
      def write(id: ID, layer: RDD[(K, V)] with Metadata[M]) = {
        val keyBounds = Bounds.fromRdd(layer).get
        LayerWriter.this.write[K, V, M](id, layer, keyIndexMethod, keyBounds)
      }
    }
}
