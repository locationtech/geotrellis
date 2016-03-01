package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import spray.json._

import scala.reflect.ClassTag

trait LayerReindexer[ID] {
  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: KeyIndexJsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndex: KeyIndex[K]): Unit

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: KeyIndexJsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
