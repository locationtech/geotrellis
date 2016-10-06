package geotrellis.spark.join

import geotrellis.util._
import geotrellis.vector._
import org.apache.spark.rdd._
import org.apache.spark.SparkContext

import scala.reflect._


abstract class VectorJoinMethods[
    L: ClassTag : ? => Geometry,
    R: ClassTag : ? => Geometry
  ] extends MethodExtensions[RDD[L]] {

  def vectorJoin(other: RDD[R], pred: (Geometry, Geometry) => Boolean)(implicit sc: SparkContext) =
    VectorJoin(self, other, pred)
}
