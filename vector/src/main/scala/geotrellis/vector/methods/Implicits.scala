/*
 * Copyright 2020 Azavea
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

package geotrellis.vector.methods

import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class withExtraPointMethods(val self: Point) extends ExtraPointMethods
  implicit class withExtraLineStringMethods(val self: LineString) extends ExtraLineStringMethods
  implicit class withExtraPolygonMethods(val self: Polygon) extends ExtraPolygonMethods
  implicit class withExtraMultiPointMethods(val self: MultiPoint) extends ExtraMultiPointMethods
  implicit class withExtraMultiLineStringMethods(val self: MultiLineString) extends ExtraMultiLineStringMethods
  implicit class withExtraMultiPolygonMethods(val self: MultiPolygon) extends ExtraMultiPolygonMethods
  implicit class withExtraGeometryMethods(val self: Geometry) extends ExtraGeometryMethods
  implicit class withExtraGeometryCollectionMethods(val self: GeometryCollection) extends ExtraGeometryCollectionMethods
}
