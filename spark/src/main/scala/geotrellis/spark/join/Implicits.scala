package geotrellis.spark.join

import geotrellis.spark._
import geotrellis.spark.partition._
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect._

object Implicits extends Implicits

trait Implicits {
  implicit class withSpatialJoinMethods[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]]
  ](val self: RDD[(K, V)] with Metadata[M])
    extends SpatialJoinMethods[K, V, M]
}
