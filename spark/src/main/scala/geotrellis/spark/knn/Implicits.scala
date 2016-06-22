package geotrellis.spark.knn

import geotrellis.vector.Point
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withKNearestMethods[T](val self: RDD[T]) extends KNearestMethods[T]
}
