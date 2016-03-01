package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import org.apache.spark.rdd._
import spray.json._
import scala.reflect._

trait LayerReader[ID] {
  val defaultNumPartitions: Int

  def read[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M]

  def read[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID): RDD[(K, V)] with Metadata[M] =
    read(id, defaultNumPartitions)

  def reader[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ]: Reader[ID, RDD[(K, V)] with Metadata[M]] =
    new Reader[ID, RDD[(K, V)] with Metadata[M]] {
      def read(id: ID): RDD[(K, V)] with Metadata[M] =
        LayerReader.this.read[K, V, M](id)
    }
}
