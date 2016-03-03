package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._

import org.apache.spark.rdd._
import spray.json._

import scala.reflect._

abstract class FilteringLayerReader[ID] extends LayerReader[ID] {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, rasterQuery: RDDQuery[K, M], numPartitions: Int): RDD[(K, V)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, rasterQuery: RDDQuery[K, M]): RDD[(K, V)] with Metadata[M] =
    read(id, rasterQuery, defaultNumPartitions)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[M] =
    read(id, new RDDQuery[K, M], numPartitions)

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](layerId: ID): BoundRDDQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _))

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](layerId: ID, numPartitions: Int): BoundRDDQuery[K, M, RDD[(K, V)] with Metadata[M]] =
    new BoundRDDQuery(new RDDQuery, read(layerId, _, numPartitions))
}
