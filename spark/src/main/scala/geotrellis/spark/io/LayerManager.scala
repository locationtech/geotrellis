package geotrellis.spark.io

import geotrellis.spark.{Boundable, LayerId}
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index._


import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait LayerManager[ID] {
  def delete(id: ID): Unit

  def copy[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: ID, to: ID): Unit

  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: ID, to: ID): Unit

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndexMethod: KeyIndexMethod[K]): Unit

  def reindex[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, keyIndex: KeyIndex[K]): Unit

}
