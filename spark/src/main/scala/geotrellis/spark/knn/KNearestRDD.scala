package geotrellis.spark.knn

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark.rdd.RDD

object KNearestRDD {
  case class Ord[T] (ex: Extent, f: T => Extent) extends Ordering[T] with Serializable {
    implicit def compare(a: T, b: T) = (ex distance (f(a))) compare (ex distance (f(b)))
  }

  def kNearest[T](rdd: RDD[T], x: Double, y: Double, k: Int)(f: T => Extent): Seq[T] =
    kNearest(rdd, new Extent(x, y, x, y), k)(f)

  def kNearest[T](rdd: RDD[T], p: (Double, Double), k: Int)(f: T => Extent): Seq[T] =
    kNearest(rdd, new Extent(p._1, p._2, p._1, p._2), k)(f)

  /**
   * Determines the k-nearest neighbors of an RDD of objects which can be
   * coerced into Extents.
   */
  def kNearest[T](rdd: RDD[T], ex: Extent, k: Int)(f: T => Extent): Seq[T] = {
    implicit val ord = new Ord[T](ex, f)

    rdd.takeOrdered(k)
  }
}
