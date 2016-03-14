package geotrellis.spark.partition

import geotrellis.spark._
import org.apache.spark.rdd._
import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withSpatiallyPartitionLayerMethods[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: Component[?, Bounds[K]]
  ](self: RDD[(K, V)] with Metadata[M])
    extends SpatiallyPartitionMethods[K, V, M](self)
}
