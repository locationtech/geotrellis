package geotrellis.layers

import geotrellis.tiling.{Bounds, Boundable}
import geotrellis.layers._
import geotrellis.layers.avro.AvroRecordCodec
import geotrellis.layers.index.{KeyIndex, KeyIndexMethod}
import geotrellis.layers.json._
import geotrellis.util._

import scala.reflect.ClassTag
import spray.json._


trait LayerReindexer[ID] {
  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, keyIndex: KeyIndex[K]): Unit

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
