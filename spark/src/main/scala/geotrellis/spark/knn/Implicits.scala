package geotrellis.spark.knn

import geotrellis.vector.{Point, Geometry, Feature}
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withKNearestMethods[T](val self: RDD[T]) extends KNearestMethods[T]
  implicit class withKNearestGeometryMethods[G <: Geometry](val self: RDD[G]) extends KNearestGeometryMethods[G]
  implicit class withKNearestFeatureMethods[G <: Geometry, D](val self: RDD[Feature[G, D]]) extends KNearestFeatureMethods[G, D]
}
