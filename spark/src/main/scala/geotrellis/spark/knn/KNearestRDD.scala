package geotrellis.spark.knn

import scala.collection.mutable.Map

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.spark.util.KryoClosure

import org.apache.spark.rdd.RDD

object KNearestRDD {
  case class Ord[T] (ex: Extent, f: T => Extent) extends Ordering[T] with Serializable {
    def compare(a: T, b: T) = (ex distance (f(a))) compare (ex distance (f(b)))
  }

  def kNearest[T](rdd: RDD[T], x: Double, y: Double, k: Int)(f: T => Extent): Seq[T] =
    kNearest(rdd, Extent(x, y, x, y), k)(f)

  def kNearest[T](rdd: RDD[T], p: (Double, Double), k: Int)(f: T => Extent): Seq[T] =
    kNearest(rdd, Extent(p._1, p._2, p._1, p._2), k)(f)

  def kNearest[T](rdd: RDD[T], p: Point, k: Int)(f: T => Extent): Seq[T] =
    kNearest(rdd, Extent(p.x, p.y, p.x, p.y), k)(f)

  /**
   * Determines the k-nearest neighbors of an RDD of objects which can be
   * coerced into Extents.
   */
  def kNearest[T](rdd: RDD[T], ex: Extent, k: Int)(f: T => Extent): Seq[T] = {
    implicit val ord = new Ord[T](ex, f)

    rdd.takeOrdered(k)
  }

  def kNearest[G, H](rdd: RDD[G], centers: Traversable[H], k: Int)(g: G => Extent, h: H => Extent): Map[H, Seq[G]] = {
    var result: Map[H, Seq[G]] = Map.empty

    centers.foreach { center =>
      implicit val ord = new Ord[G](h(center), g)

      result(center) = rdd.takeOrdered(k)
    }
    result
  }

/*  def kNearest[G <: Geometry, H <: Geometry](rdd: RDD[G], centers: Traversable[H], k: Int): Map[H, Seq[G]] = {
    var result: Map[H, Seq[G]] = Map.empty

    centers.foreach { center =>
      implicit val ord = new Ord[G](center.envelope, _.envelope)

      result(center) = rdd.takeOrdered(k)
    }
    result
  }

  def kNearest[G <: Geometry, H <: Geometry, F](rdd: RDD[G], centers: Traversable[Feature[H,F]], k: Int)
      (implicit d: DummyImplicit): Map[Feature[H,F], Seq[G]] = {
    var result: Map[Feature[H, F], Seq[G]] = Map.empty

    centers.foreach { center =>
      implicit val ord = new Ord[G](center.geom.envelope, _.envelope)

      result(center) = rdd.takeOrdered(k)
    }
    result
  }

  def kNearest[G <: Geometry, D, H <: Geometry, F](rdd: RDD[Feature[G, D]], centers: Traversable[Feature[H, F]], k: Int)
      (implicit d1: DummyImplicit, d2: DummyImplicit): Map[Feature[H, F], Seq[Feature[G, D]]] = {
    var result: Map[Feature[H, F], Seq[Feature[G, D]]] = Map.empty

    centers.foreach { center =>
      implicit val ord = new Ord[Feature[G,D]](center.geom.envelope, _.geom.envelope)

      result(center) = rdd.takeOrdered(k)
    }
    result
  }

  def kNearest[G <: Geometry, D, H <: Geometry](rdd: RDD[Feature[G, D]], centers: Traversable[H], k: Int)
      (implicit d1: DummyImplicit, d2: DummyImplicit, d3: DummyImplicit): Map[H, Seq[Feature[G, D]]] = {
    var result: Map[H, Seq[Feature[G, D]]] = Map.empty

    centers.foreach { center =>
      implicit val ord = new Ord[Feature[G, D]](center.envelope, _.geom.envelope)

      result(center) = rdd.takeOrdered(k)
    }
    result
  }*/
}
