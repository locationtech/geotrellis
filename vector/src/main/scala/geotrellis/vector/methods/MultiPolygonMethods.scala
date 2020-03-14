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

trait ExtraMultiPolygonMethods extends MethodExtensions[MultiPolygon] {
  def polygons: Array[Polygon] = {
    for (i <- 0 until self.getNumGeometries) yield {
      self.getGeometryN(i).copy.asInstanceOf[Polygon]
    }
  }.toArray

  def &(p: Point): PointOrNoResult = self.intersection(p)
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def &(l: LineString): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(l)
  def &(ml: MultiLineString): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(ml)
  def &(p: Polygon): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(p)
  def &(mp: MultiPolygon): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(mp)
  def &(ex: Extent): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(ex.toPolygon)

  def -(p: Point): MultiPolygonXDifferenceResult = self.difference(p)
  def -(mp: MultiPoint): MultiPolygonXDifferenceResult = self.difference(mp)
  def -(l: LineString): MultiPolygonXDifferenceResult = self.difference(l)
  def -(ml: MultiLineString): MultiPolygonXDifferenceResult = self.difference(ml)
  def -(p: Polygon): TwoDimensionsTwoDimensionsDifferenceResult = self.difference(p)
  def -(mp: MultiPolygon): TwoDimensionsTwoDimensionsDifferenceResult = self.difference(mp)

  def |(p: Point): PointMultiPolygonUnionResult = self.union(p)
  def |(mp: MultiPoint): LineStringMultiPolygonUnionResult = self.union(mp)
  def |(l: LineString): LineStringMultiPolygonUnionResult = self.union(l)
  def |(ml: MultiLineString): LineStringMultiPolygonUnionResult = self.union(ml)
  def |(p: Polygon): TwoDimensionsTwoDimensionsSeqUnionResult = (this.polygons :+ p).toSeq.unionGeometries
  def |(mp: MultiPolygon): TwoDimensionsTwoDimensionsSeqUnionResult = (this.polygons ++ mp.polygons).toSeq.unionGeometries

  def normalized(): MultiPolygon = {
    val res = self.copy.asInstanceOf[MultiPolygon]
    res.normalize
    res
  }
}
