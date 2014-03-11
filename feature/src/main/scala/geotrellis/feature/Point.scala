/***
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
 ***/

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Point {

  def apply(x: Double, y: Double): Point =
    Point(factory.createPoint(new jts.Coordinate(x, y)))

  implicit def jts2Point(jtsGeom: jts.Point): Point = apply(jtsGeom)

}

case class Point(jtsGeom: jts.Point) extends Geometry
                                     with Relatable
                                     with ZeroDimensions {

  assert(!jtsGeom.isEmpty)

  val x: Double =
    jtsGeom.getX
  val y: Double =
    jtsGeom.getY

  // -- Intersection

  def &(other: Geometry): PointOrNoResult =
    intersection(other)
  def intersection(other: Geometry): PointOrNoResult =
    jtsGeom.intersection(other.jtsGeom)

  // -- Union

  def |(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    union(g)
  def union(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    jtsGeom.union(g.jtsGeom)

  def |(l: Line): PointLineUnionResult =
    union(l)
  def union(l: Line): PointLineUnionResult =
    jtsGeom.union(l.jtsGeom)

  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  def |(ls: MultiLine): PointMultiLineUnionResult =
    union(ls)
  def union(ls: MultiLine): PointMultiLineUnionResult =
    jtsGeom.union(ls.jtsGeom)

  def |(ps: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

  // -- Difference

  def -(other: Geometry): PointGeometryDifferenceResult =
    difference(other)
  def difference(other: Geometry): PointGeometryDifferenceResult =
    jtsGeom.difference(other.jtsGeom)

  // -- SymDifference

  def symDifference(p: Point): PointPointSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(l.jtsGeom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  def symDifference(ps: MultiPoint): ZeroDimensionsMultiPointSymDifferenceResult =
    jtsGeom.symDifference(ps.jtsGeom)

  def symDifference(ls: MultiLine): ZeroDimensionsMultiLineSymDifferenceResult =
    jtsGeom.symDifference(ls.jtsGeom)

  def symDifference(ps: MultiPolygon): ZeroDimensionsMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(ps.jtsGeom)

  // -- Buffer

  def buffer(d: Double): Polygon = {
    val result = jtsGeom.buffer(d)
    result match {
      case p: jts.Polygon => Polygon(p)
      case _ =>
        sys.error(s"Unexpected result for Point buffer: ${result.getGeometryType}")
    }
  }

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: Geometry): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(p: Point): Boolean =
    jtsGeom.covers(p.jtsGeom)

  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: Geometry): Boolean =
    jtsGeom.within(g.jtsGeom)

}
