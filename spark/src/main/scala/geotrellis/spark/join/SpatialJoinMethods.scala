package geotrellis.spark.join

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.partition._
import org.apache.spark.rdd._
import geotrellis.util._

import scala.reflect._

abstract class SpatialJoinMethods[
  K: Boundable: PartitionerIndex: ClassTag,
  V: ClassTag,
  M: GetComponent[?, Bounds[K]]
] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def spatialLeftOuterJoin[W: ClassTag, M1: Component[?, Bounds[K]]](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, Option[W]))] with Metadata[Bounds[K]] =
    SpatialJoin.leftOuterJoin(self, right)

  def spatialJoin[W: ClassTag, M1: Component[?, Bounds[K]]](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, W))] with Metadata[Bounds[K]] =
    SpatialJoin.join(self, right)
}
