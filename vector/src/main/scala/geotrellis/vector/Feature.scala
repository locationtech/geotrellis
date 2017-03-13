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

/** A case class which represents a geometry with some metadata
  *
  * @tparam G A subtype of Geometry
  * @tparam D The type of any provided metadata
  * @param geom An instance of G
  * @param data An instance of D
  */
case class Feature[+G <: Geometry, +D](geom: G, data: D) {

  /** Method for manipulating this class' geom
    * @tparam T A subtype of Geometry
    * @param f A function from G to T
    */
  def mapGeom[T <: Geometry](f: G => T): Feature[T, D] =
    Feature(f(geom), data)

  /** Method for manipulating this class' data
    * @tparam T The type of the data expected
    * @param f A function from D to T
    */
  def mapData[T](f: D => T): Feature[G, T] =
    Feature(geom, f(data))
}

/** Feature companion object */
object Feature {
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
