package geotrellis.spark.knn

import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, Point, Geometry, Feature}

import org.apache.spark.rdd.RDD

trait KNearestMethods[T] extends MethodExtensions[RDD[T]] {
  def kNearest(x: Double, y: Double, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, x, y, k)(f) }

  def kNearest(p: (Double, Double), k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(p: Point, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(ex: Extent, k: Int)(f: T => Extent): Seq[T] = { KNearestRDD.kNearest[T](self, ex, k)(f) }
}

trait KNearestGeometryMethods[G <: Geometry] extends MethodExtensions[RDD[G]] {
  def kNearest(x: Double, y: Double, k: Int):Seq[G] = KNearestRDD.kNearest[G](self, x, y, k){ g: G => g.envelope }

  def kNearest(p: (Double, Double), k: Int):Seq[G] = KNearestRDD.kNearest[G](self, p, k){ g: G => g.envelope }

  def kNearest(p: Point, k: Int):Seq[G] = KNearestRDD.kNearest[G](self, p, k){ g: G => g.envelope }

  def kNearest(ex: Extent, k: Int):Seq[G] = KNearestRDD.kNearest[G](self, ex, k){ g: G => g.envelope }
}

trait KNearestFeatureMethods[G <: Geometry, D] extends MethodExtensions[RDD[Feature[G, D]]] {
  def kNearest(x: Double, y: Double, k: Int):Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, x, y, k){ g: Feature[G, D] => g.geom.envelope }

  def kNearest(p: (Double, Double), k: Int):Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, p, k){ g: Feature[G, D] => g.geom.envelope }

  def kNearest(p: Point, k: Int):Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, p, k){ g: Feature[G, D] => g.geom.envelope }

  def kNearest(ex: Extent, k: Int):Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, ex, k){ g: Feature[G, D] => g.geom.envelope }
}
