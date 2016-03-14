package geotrellis.spark.partition

import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class SpatiallyPartitionMethods[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: Component[?, Bounds[K]]
  ](val self: RDD[(K, V)] with Metadata[M]) extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def spatiallyPartition(): RDD[(K, V)] with Metadata[Bounds[K]] =
    SpatiallyPartition(self)

  def spatiallyPartition(filterBounds: KeyBounds[K]): RDD[(K, V)] with Metadata[Bounds[K]] =
    SpatiallyPartition(self, filterBounds)
}
