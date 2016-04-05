package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._

import spray.json._

import scala.reflect._

trait TileReader[ID] {
  def read[K: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec](layerId: ID): Reader[K, V]
}
