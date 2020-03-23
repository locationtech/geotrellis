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

trait ExtraMultiPointMethods extends MethodExtensions[MultiPoint] {
  def points: Array[Point] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Point]
    }
  }.toArray

  def &(p: Point): PointOrNoResult = self.intersection(p)
  def &(mp: MultiPoint): MultiPointMultiPointIntersectionResult = self.intersection(mp)
  def &[G <: Geometry : AtLeastOneDimension](g: G): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(g)
  def &(ex: Extent): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(ex.toPolygon)

  def -(g: Geometry): MultiPointGeometryDifferenceResult = self.difference(g)

  def |(p: Point): PointZeroDimensionsUnionResult = self.union(p)
  def |(mp: MultiPoint): MultiPointMultiPointUnionResult = self.union(mp)
  def |(l: LineString): ZeroDimensionsLineStringUnionResult = self.union(l)
  def |(ml: MultiLineString): MultiPointMultiLineStringUnionResult= self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): MultiPointMultiPolygonUnionResult = self.union(mp)

  def normalized(): MultiPoint = {
    val res = self.copy.asInstanceOf[MultiPoint]
    res.normalize
    res
  }
}
