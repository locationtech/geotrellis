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

import scala.collection.immutable.Map

import geotrellis.util.MethodExtensions
import geotrellis.vector.{Extent, Point, Geometry, Feature}

import org.apache.spark.rdd.RDD

trait KNearestMethods[T] extends MethodExtensions[RDD[T]] {
  def kNearest(x: Double, y: Double, k: Int)(f: T => Geometry): Seq[T] = { KNearestRDD.kNearest[T](self, x, y, k)(f) }

  def kNearest(p: (Double, Double), k: Int)(f: T => Geometry): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(p: Point, k: Int)(f: T => Geometry): Seq[T] = { KNearestRDD.kNearest[T](self, p, k)(f) }

  def kNearest(ex: Extent, k: Int)(f: T => Geometry): Seq[T] = { KNearestRDD.kNearest[T](self, ex, k)(f) }

  def kNearest[H <: Geometry](centers: Traversable[H], k: Int)(f: T => Geometry): Seq[Seq[T]] = {
    KNearestRDD.kNearest[T, H](self, centers, k)(f, _.envelope)
  }

  def kNearest[H <: Geometry, F](centers: Traversable[Feature[H, F]], k: Int)(f: T => Geometry)(implicit d: DummyImplicit): Seq[Seq[T]] = {
    KNearestRDD.kNearest[T, Feature[H, F]](self, centers, k)(f, _.geom.envelope)
  }
}

trait KNearestGeometryMethods[G <: Geometry] extends MethodExtensions[RDD[G]] {
  def kNearest(x: Double, y: Double, k: Int): Seq[G] = KNearestRDD.kNearest[G](self, x, y, k){ g: G => g }

  def kNearest(p: (Double, Double), k: Int): Seq[G] = KNearestRDD.kNearest[G](self, p, k){ g: G => g }

  def kNearest(p: Point, k: Int): Seq[G] = KNearestRDD.kNearest[G](self, p, k){ g: G => g }

  def kNearest(ex: Extent, k: Int): Seq[G] = KNearestRDD.kNearest[G](self, ex, k){ g: G => g }

  def kNearest[H <: Geometry](centers: Traversable[H], k: Int): Seq[Seq[G]] = {
    KNearestRDD.kNearest[G, H](self, centers, k)(x => x, x => x)
  }

  def kNearest[H <: Geometry, F](centers: Traversable[Feature[H, F]], k: Int)(implicit d: DummyImplicit): Seq[Seq[G]] = {
    KNearestRDD.kNearest[G, Feature[H, F]](self, centers, k)(x => x, _.geom)
  }
}

trait KNearestFeatureMethods[G <: Geometry, D] extends MethodExtensions[RDD[Feature[G, D]]] {
  def kNearest(x: Double, y: Double, k: Int): Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, x, y, k){ g: Feature[G, D] => g.geom }

  def kNearest(p: (Double, Double), k: Int): Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, p, k){ g: Feature[G, D] => g.geom }

  def kNearest(p: Point, k: Int): Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, p, k){ g: Feature[G, D] => g.geom }

  def kNearest(ex: Extent, k: Int): Seq[Feature[G, D]] = 
    KNearestRDD.kNearest[Feature[G, D]](self, ex, k){ g: Feature[G, D] => g.geom }

  def kNearest[H <: Geometry](centers: Traversable[H], k: Int): Seq[Seq[Feature[G, D]]] = {
    KNearestRDD.kNearest[Feature[G, D], H](self, centers, k)(_.geom, x => x)
  }

  def kNearest[H <: Geometry, F](centers: Traversable[Feature[H, F]], k: Int)(implicit d: DummyImplicit): Seq[Seq[Feature[G, D]]] = {
    KNearestRDD.kNearest[Feature[G, D], Feature[H, F]](self, centers, k)(_.geom, _.geom)
  }
}
