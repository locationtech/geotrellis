package geotrellis.spark.io

import geotrellis.spark._

import geotrellis.spark._
import geotrellis.spark.io.avro._

import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect.ClassTag

abstract class LayerUpdater[ID] {
  def update[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](id: ID, rdd: RDD[(K, V)] with Metadata[M]): Unit

  def mergeUpdate[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ] (id: ID, reader: FilteringLayerReader[ID], rdd: RDD[(K, V)] with Metadata[M])
    (merge: (RDD[(K, V)] with Metadata[M], RDD[(K, V)] with Metadata[M]) => RDD[(K, V)] with Metadata[M]) = {
    val existing = reader.query[K, V, M](id).where(Intersects(Boundable.getKeyBounds(rdd))).toRDD
    update(id, merge(existing, rdd))
  }
}
