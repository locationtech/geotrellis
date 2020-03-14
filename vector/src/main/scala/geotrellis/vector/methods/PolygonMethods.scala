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
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.operation.union._
import spire.syntax.cfor._

import scala.collection.JavaConverters._

trait ExtraPolygonMethods extends MethodExtensions[Polygon] {
  def exterior: LineString =
    self.getExteriorRing.copy.asInstanceOf[LineString]

  def holes: Array[LineString] =
  (for (i <- 0 until self.getNumInteriorRing) yield self.getInteriorRingN(i).copy.asInstanceOf[LineString]).toArray

  private def populatePoints(sequence: CoordinateSequence, arr: Array[Point], offset: Int = 0): Array[Point] = {
    cfor(0)(_ < sequence.size, _ + 1) { i =>
      arr(i + offset) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  def vertices: Array[Point] = {
    val arr = Array.ofDim[Point](self.getNumPoints)

    val sequences = self.getExteriorRing.getCoordinateSequence +: (0 until self.getNumInteriorRing).map(self
      .getInteriorRingN(_).getCoordinateSequence)
    val offsets = sequences.map(_.size).scanLeft(0)(_ + _).dropRight(1)

    sequences.zip(offsets).foreach { case (seq, offset) => populatePoints(seq, arr, offset) }

    arr
  }

  def &(p: Point): PointOrNoResult = self.intersection(p)
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult = self.intersection(mp)
  def &(l: LineString): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(l)
  def &(ml: MultiLineString): OneDimensionAtLeastOneDimensionIntersectionResult = self.intersection(ml)
  def &(p: Polygon): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(p)
  def &(mp: MultiPolygon): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(mp)
  def &(ex: Extent): TwoDimensionsTwoDimensionsIntersectionResult = self.intersection(ex.toPolygon)

  def -(p: Point): PolygonAtMostOneDimensionDifferenceResult = self.difference(p)
  def -(mp: MultiPoint): PolygonAtMostOneDimensionDifferenceResult = self.difference(mp)
  def -(l: LineString): PolygonAtMostOneDimensionDifferenceResult = self.difference(l)
  def -(ml: MultiLineString): PolygonAtMostOneDimensionDifferenceResult = self.difference(ml)
  def -(p: Polygon): TwoDimensionsTwoDimensionsDifferenceResult = self.difference(p)
  def -(mp: MultiPolygon): TwoDimensionsTwoDimensionsDifferenceResult = self.difference(mp)

  def |(p: Point): AtMostOneDimensionPolygonUnionResult = self.union(p)
  def |(mp: MultiPoint): AtMostOneDimensionPolygonUnionResult = self.union(mp)
  def |(l: LineString): AtMostOneDimensionPolygonUnionResult = self.union(l)
  def |(ml: MultiLineString): AtMostOneDimensionPolygonUnionResult = self.union(ml)
  def |[G <: Geometry : TwoDimensional](g: G): TwoDimensionsTwoDimensionsUnionResult =  g match {
    case p: Polygon =>
      new CascadedPolygonUnion(Seq(self, p).asJava).union
    case mp: MultiPolygon =>
      new CascadedPolygonUnion((self +: mp.polygons).toSeq.asJava).union
    case _ =>
      self.union(g)
}

  def normalized(): Polygon = {
    val res = self.copy.asInstanceOf[Polygon]
    res.normalize
    res
  }
}
