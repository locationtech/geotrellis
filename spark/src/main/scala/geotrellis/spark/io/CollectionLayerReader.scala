package geotrellis.spark.io
import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.json._
import geotrellis.util._

import spray.json._

import scala.reflect._

abstract class CollectionLayerReader[ID] {
  val defaultNumPartitions = 1

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], numPartitions: Int, indexFilterOnly: Boolean): Seq[(K, V)] with Metadata[M]

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M], numPartitions: Int): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, rasterQuery, numPartitions, false)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, rasterQuery: LayerQuery[K, M]): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, rasterQuery, defaultNumPartitions)

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: ID, numPartitions: Int): Seq[(K, V)] with Metadata[M] =
    read[K, V, M](id, new LayerQuery[K, M], numPartitions)

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](layerId: ID): BoundLayerQuery[K, M, Seq[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read[K, V, M](layerId, _))

  def query[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](layerId: ID, numPartitions: Int): BoundLayerQuery[K, M, Seq[(K, V)] with Metadata[M]] =
    new BoundLayerQuery(new LayerQuery, read[K, V, M](layerId, _, numPartitions))
}
