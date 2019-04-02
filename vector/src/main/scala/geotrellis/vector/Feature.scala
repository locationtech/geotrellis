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

package geotrellis.vector

import cats._
import cats.implicits._

/** A case class which represents a geometry with some metadata
  *
  * @tparam G A subtype of Geometry
  * @tparam D The type of any provided metadata
  * @param geom An instance of G
  * @param data An instance of D
  */
class Feature[+G <: Geometry, +D](val geom: G, val data: D)

/** Feature companion object */
object Feature {
  def apply[G <: Geometry, D](geom: G, data: D) = new Feature(geom, data)

  type FeatureGeometry[D] = ({ type Type[G <: Geometry] = Feature[G, D] })

  implicit def featureHasGeometry[D] = new HasGeometry[FeatureGeometry[D]#Type] {
    def geom[G <: Geometry](ft: Feature[G, D]): G = ft.geom
    def mapGeom[G <: Geometry, T <: Geometry](ft: Feature[G, D])(fn: G => T): Feature[T, D] = Feature(fn(ft.geom), ft.data)
  }

  implicit def featureFunctor[G <: Geometry] = new Functor[Feature[G, ?]] {
    override def map[A, B](feature: Feature[G, A])(f: A => B): Feature[G, B] = Feature(feature.geom, f(feature.data))
  }

  implicit def featureToGeometry[G <: Geometry](f: Feature[G, _]): G = f.geom
}

/** PointFeature companion object */
object PointFeature {
  def apply[D](geom: Point, data: D): Feature[Point, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[Point, D]) =
    Some(feature.geom -> feature.data)
}

/** LineFeature companion object */
object LineFeature {
  def apply[D](geom: Line, data: D): Feature[Line, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[Line, D]) =
    Some(feature.geom -> feature.data)
}

/** PolygonFeature companion object */
object PolygonFeature {
  def apply[D](geom: Polygon, data: D): Feature[Polygon, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[Polygon, D]) =
    Some(feature.geom -> feature.data)
}

/** MultiPointFeature companion object */
object MultiPointFeature {
  def apply[D](geom: MultiPoint, data: D): Feature[MultiPoint, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[MultiPoint, D]) =
    Some(feature.geom -> feature.data)
}

/** MultiLineFeature companion object */
object MultiLineFeature {
  def apply[D](geom: MultiLine, data: D): Feature[MultiLine, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[MultiLine, D]) =
    Some(feature.geom -> feature.data)
}

/** MultiPolygonFeature companion object */
object MultiPolygonFeature {
  def apply[D](geom: MultiPolygon, data: D): Feature[MultiPolygon, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[MultiPolygon, D]) =
    Some(feature.geom -> feature.data)
}

/** GeometryCollectionFeature companion object */
object GeometryCollectionFeature {
  def apply[D](geom: GeometryCollection, data: D): Feature[GeometryCollection, D] =
    Feature(geom, data)

  def unapply[D](feature: Feature[GeometryCollection, D]) =
    Some(feature.geom -> feature.data)
}
