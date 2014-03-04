/*******************************************************************************
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Point {

  def apply(x: Double, y: Double): Point =
    Point(factory.createPoint(new jts.Coordinate(x, y)))

  implicit def jts2Point(geom: jts.Point): Point = apply(geom)

}

case class Point(geom: jts.Point) extends Geometry 
                                     with ZeroDimensions {

  assert(!geom.isEmpty)

  val x: Double =
    geom.getX
  val y: Double =
    geom.getY

  // -- Intersection

  def &(other: Geometry): PointGeometryIntersectionResult =
    intersection(other)
  def intersection(other: Geometry): PointGeometryIntersectionResult =
    geom.intersection(other.geom)

  // -- Union

  def |(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    union(g)
  def union(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    geom.union(g.geom)

  def |(l: Line): PointLineUnionResult =
    union(l)
  def union(l: Line): PointLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    geom.union(p.geom)

  def |(ls: LineSet): PointLineSetUnionResult =
    union(ls)
  def union(ls: LineSet): PointLineSetUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    union(ps)
  def union(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry): PointGeometryDifferenceResult =
    difference(other)
  def difference(other: Geometry): PointGeometryDifferenceResult =
    geom.difference(other.geom)

  // -- SymDifference

  def symDifference(p: Point): PointPointSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(l.geom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ps: PointSet): ZeroDimensionsPointSetSymDifferenceResult =
    geom.symDifference(ps.geom)

  def symDifference(ls: LineSet): ZeroDimensionsLineSetSymDifferenceResult =
    geom.symDifference(ls.geom)

  def symDifference(ps: PolygonSet): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(ps.geom)

  // -- Buffer

  def buffer(d: Double): Polygon = {
    val result = geom.buffer(d)
    result match {
      case p: jts.Polygon => Polygon(p)
      case _ =>
        sys.error(s"Unexpected result for Point buffer: ${result.getGeometryType}")
    }
  }

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    geom.contains(g.geom)

  def within(g: Geometry): Boolean =
    geom.within(g.geom)

}
