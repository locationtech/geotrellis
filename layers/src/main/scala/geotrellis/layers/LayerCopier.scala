package geotrellis.layers

import geotrellis.tiling.{Bounds, Boundable}
import geotrellis.layers._
import geotrellis.layers.avro.AvroRecordCodec
import geotrellis.util._

import scala.reflect.ClassTag
import spray.json._


trait LayerCopier[ID] {
  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: ID, to: ID): Unit
}
