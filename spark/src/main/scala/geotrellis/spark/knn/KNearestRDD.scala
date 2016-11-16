/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.knn

import geotrellis.spark._
import geotrellis.vector._

import org.apache.spark.rdd.RDD

import java.util.PriorityQueue
import scala.collection.JavaConversions._

class BoundedPriorityQueue[A: Ordering](val maxSize: Int) extends Serializable{
  val pq = new PriorityQueue[A](maxSize, implicitly[Ordering[A]].reverse)

  def +=(a: A): this.type = {
    if (pq.size < maxSize) pq.add(a)
    else maybeReplaceLowest(a)
    this
  }

  def ++=(xs: TraversableOnce[A]): this.type = {
    xs.foreach { this += _ }
    this
  }

  def enqueue(elems: A*) = {
    this ++= elems
  }

  def peek() = pq.peek

  def iterator() = pq.iterator

  private def maybeReplaceLowest(a: A) = {
    if (pq.comparator.compare(a, peek) > 0) {
      pq.poll
      pq.add(a)
    }
  }

}

object BoundedPriorityQueue {
  /**
   * Creates a new BoundedPriorityQueue instance.
   * @param maximum the max number of elements
   * @return a new bounded priority queue instance
   */
  def apply[A: Ordering](maximum: Int): BoundedPriorityQueue[A] = {
    new BoundedPriorityQueue(maximum)
  }
}

object KNearestRDD {
  case class Ord[T] (ex: Geometry, f: T => Geometry) extends Ordering[T] with Serializable {
    def compare(a: T, b: T) = (ex distance (f(a))) compare (ex distance (f(b)))
  }

  def kNearest[T](rdd: RDD[T], x: Double, y: Double, k: Int)(f: T => Geometry): Seq[T] =
    kNearest(rdd, Extent(x, y, x, y), k)(f)

  def kNearest[T](rdd: RDD[T], p: (Double, Double), k: Int)(f: T => Geometry): Seq[T] =
    kNearest(rdd, Extent(p._1, p._2, p._1, p._2), k)(f)

  def kNearest[T](rdd: RDD[T], p: Point, k: Int)(f: T => Geometry): Seq[T] =
    kNearest(rdd, Extent(p.x, p.y, p.x, p.y), k)(f)

  /**
   * Determines the k-nearest neighbors of an RDD of objects which can be
   * coerced into Extents.
   */
  def kNearest[T](rdd: RDD[T], ex: Extent, k: Int)(f: T => Geometry): Seq[T] = {
    implicit val ord = new Ord[T](ex, f)

    rdd.takeOrdered(k)
  }

  def kNearest[G, H](rdd: RDD[G], centers: Traversable[H], k: Int)(g: G => Geometry, h: H => Geometry): Seq[Seq[G]] = {
    var zero: Traversable[BoundedPriorityQueue[G]] = centers.map { center =>
      implicit val ord = new Ord[G](h(center), g)
      BoundedPriorityQueue[G](k)
    }
    def zipWith[A, T](l: Seq[A], r: Seq[A])(f: (A, A) => T): Seq[T] = {
      (l, r) match {
        case (Nil, _) => Nil
        case (_, Nil) => Nil
        case (ll :: lls, rr :: rrs) => f(ll, rr) +: zipWith(lls, rrs)(f)
      }
    }
    def merge(a: BoundedPriorityQueue[G], b: BoundedPriorityQueue[G]) = {
      implicit val ord: Ordering[G] = a.pq.comparator.asInstanceOf[Ordering[G]].reverse
      val result = BoundedPriorityQueue[G](a.maxSize)
      a.iterator.foreach { item => result += item }
      b.iterator.foreach { item => result += item }
      result
    }

    val result = rdd.aggregate(zero)({ (bpqs, toAdd) => bpqs.map { _ += toAdd } }, { (a, b) => zipWith(a.toSeq, b.toSeq)(merge).toTraversable })
    result.map(_.iterator.toList).toList
  }
}

