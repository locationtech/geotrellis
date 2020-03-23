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
import geotrellis.util.MethodExtensions

trait ExtraPointMethods extends MethodExtensions[Point] {
  def x: Double = self.getCoordinate.getX
  def y: Double = self.getCoordinate.getY

  def &(g: Geometry): PointOrNoResult = self.intersection(g)
  def &(ex: Extent): PointOrNoResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): PointGeometryDifferenceResult = self.difference(g)

  def |(p: Point): PointZeroDimensionsUnionResult = self.union(p)
  def |(mp: MultiPoint): PointZeroDimensionsUnionResult = self.union(mp)
  def |(l: LineString): ZeroDimensionsLineStringUnionResult = self.union(l)
  def |(ml: MultiLineString): PointMultiLineStringUnionResult = self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): PointMultiPolygonUnionResult = self.union(mp)
}
