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
import org.locationtech.jts.geom.Coordinate
import spire.syntax.cfor._

trait ExtraLineStringMethods extends MethodExtensions[LineString] {
  def closed(): LineString = {
    if (self.isClosed)
      self.copy.asInstanceOf[LineString]
    else {
      val arr = Array.ofDim[Coordinate](self.getNumPoints + 1)

      cfor(0)(_ < self.getNumPoints, _ + 1) { i =>
        arr(i) = self.getCoordinateN(i)
      }
      arr(self.getNumPoints) = self.getCoordinateN(0)

      GeomFactory.factory.createLineString(arr)
    }
  }

  def points: Array[Point] = {
    val arr = Array.ofDim[Point](self.getNumPoints)
    val sequence = self.getCoordinateSequence

    cfor(0)(_ < arr.length, _ + 1) { i =>
      arr(i) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  def &(p: Point): PointOrNoResult = self.intersection(p)
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def &[G <: Geometry : AtLeastOneDimension](g: G): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(g)
  def &(ex: Extent): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(ex.toPolygon)

  def -(p: Point): LineStringResult = self.difference(p)
  def -(mp: MultiPoint): LineStringResult = self.difference(mp)
  def -(l: LineString): LineStringAtLeastOneDimensionDifferenceResult = self.difference(l)
  def -(ml: MultiLineString): LineStringAtLeastOneDimensionDifferenceResult = self.difference(ml)
  def -(p: Polygon): LineStringAtLeastOneDimensionDifferenceResult = self.difference(p)
  def -(mp: MultiPolygon): LineStringAtLeastOneDimensionDifferenceResult = self.difference(mp)

  def |(p: Point): ZeroDimensionsLineStringUnionResult = self.union(p)
  def |(mp: MultiPoint): ZeroDimensionsLineStringUnionResult = self.union(mp)
  def |(l: LineString): LineStringOneDimensionUnionResult = self.union(l)
  def |(ml: MultiLineString): LineStringOneDimensionUnionResult = self.union(ml)
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPolygon): LineStringMultiPolygonUnionResult = self.union(mp)

  def normalized(): LineString = {
    val res = self.copy.asInstanceOf[LineString]
    res.normalize
    res
  }
}
