package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._

import spray.json._

import scala.reflect.ClassTag

trait LayerMover[ID] {
  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: ID, to: ID): Unit
}
