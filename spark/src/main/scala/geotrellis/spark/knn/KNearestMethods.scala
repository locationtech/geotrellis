package geotrellis.spark.knn

import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, Point}

import org.apache.spark.rdd.RDD

trait KNearestMethods[T] extends MethodExtensions[RDD[T]] {
  def kNearest(x: Double, y: Double, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, x, y, k)(f) }

  def kNearest(p: (Double, Double), k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(p: Point, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(ex: Extent, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, ex, k)(f) }
}
