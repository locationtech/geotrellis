package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._

import spray.json._

import scala.reflect.ClassTag

trait LayerCopier[ID] {
  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: KeyIndexJsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: ID, to: ID): Unit
}
