package geotrellis.layers

import geotrellis.tiling.{Bounds, Boundable}
import geotrellis.layers._
import geotrellis.layers.avro.AvroRecordCodec
import geotrellis.layers.index._
import geotrellis.layers.json._
import geotrellis.util._

import scala.reflect.ClassTag
import spray.json._


trait LayerManager[ID] {
  def delete(id: ID): Unit

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: ID, to: ID): Unit

  def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: ID, to: ID): Unit

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, keyIndex: KeyIndex[K]): Unit

}
