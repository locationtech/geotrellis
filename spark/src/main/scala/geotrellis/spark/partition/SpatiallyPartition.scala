package geotrellis.spark.partition

import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

object SpatiallyPartition {
  def apply[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M]): RDD[(K, V)] with Metadata[Bounds[K]] =
    apply(rdd, rdd.metadata.getComponent[Bounds[K]])

  def apply[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M], filterBounds: Bounds[K]): RDD[(K, V)] with Metadata[Bounds[K]] = {
    val partitioner = SpacePartitioner(filterBounds)
    partitioner(rdd)
  }
}
