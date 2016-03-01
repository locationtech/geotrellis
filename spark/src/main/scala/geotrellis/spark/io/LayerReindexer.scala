package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import spray.json._

import scala.reflect.ClassTag

trait LayerReindexer[ID] {
  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndex: KeyIndex[K]): Unit

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit
}
