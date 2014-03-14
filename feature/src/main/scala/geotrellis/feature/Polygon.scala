/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Polygon {

  implicit def jtsToPolygon(jtsGeom: jts.Polygon): Polygon =
    Polygon(jtsGeom)

  def apply(exterior: Line): Polygon =
    apply(exterior, Set())

  def apply(exterior: Line, holes:Set[Line]): Polygon = {
    if(!exterior.isClosed) {
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior")
    }

    if(exterior.points.length < 4) {
      sys.error(s"Cannot create a polygon with exterior with less that 4 points: $exterior")
    }

    val extGeom = factory.createLinearRing(exterior.jtsGeom.getCoordinates)

    val holeGeoms = (
      for (hole <- holes) yield {
        if (!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if (hole.points.length < 4)
            sys.error(s"Cannot create a polygon with a hole with less that 4 points: $hole")
          else
            factory.createLinearRing(hole.jtsGeom.getCoordinates)
        }
      }).toArray

    factory.createPolygon(extGeom, holeGeoms)
  }

}

case class Polygon(jtsGeom: jts.Polygon) extends Geometry 
                                         with Relatable
                                         with TwoDimensions {

  assert(!jtsGeom.isEmpty)
  assert(jtsGeom.isValid)

  /**
   * Tests whether this Polygon is a rectangle.
   */
  lazy val isRectangle: Boolean =
    jtsGeom.isRectangle

  /**
   * Returns the area of this Polygon.
   */
  lazy val area: Double =
    jtsGeom.getArea

  /**
   * Returns the exterior ring of this Polygon.
   */
  lazy val exterior: Line =
    Line(jtsGeom.getExteriorRing)

  /**
   * Returns the boundary of this Polygon.
   * The boundary of a Polygon is the set of closed curves corresponding to its
   * exterior and interior boundaries.
   */
  lazy val boundary: PolygonBoundaryResult =
    jtsGeom.getBoundary

  /**
   * Returns this Polygon's vertices.
   */
  lazy val vertices: MultiPoint =
    jtsGeom.getCoordinates

  /**
   * Returns a Polygon whose points are (minx, miny), (minx, maxy),
   * (maxx, maxy), (maxx, miny), (minx, miny).
   */
  lazy val boundingBox: Polygon =
    jtsGeom.getEnvelope match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for Polygon boundingBox: ${x.getGeometryType}")
    }

  /**
   * Returns this Polygon's perimeter.
   * A Polygon's perimeter is the length of its exterior and interior
   * boundaries.
   */
  lazy val perimeter: Double =
    jtsGeom.getLength

  // -- Intersection

  def &(p: Point): PointOrNoResult =
    intersection(p)
  def intersection(p: Point): PointOrNoResult =
    p.intersection(this)

  def &(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(l)
  def intersection(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): PolygonPolygonIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): PolygonPolygonIntersectionResult =
    jtsGeom.intersection(p.jtsGeom)

  def &(ps: MultiPoint): MultiPointGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPoint): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  def &(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(ls.jtsGeom)

  def &(ps: MultiPolygon): MultiPolygonIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiPolygonIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  // -- Union

  def |(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    union(g)
  def union(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(g.jtsGeom)

  def |(p:Polygon): PolygonPolygonUnionResult =
    union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  def |(ps: MultiPolygon): PolygonPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): PolygonPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

  // -- Difference

  def -(p: Point): PolygonXDifferenceResult =
    difference(p)
  def difference(p: Point): PolygonXDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(l: Line): PolygonXDifferenceResult =
    difference(l)
  def difference(l: Line): PolygonXDifferenceResult =
    jtsGeom.difference(l.jtsGeom)

  def -(p: Polygon): PolygonPolygonDifferenceResult =
    difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(ps: MultiPoint): PolygonXDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): PolygonXDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  def -(ls: MultiLine): PolygonXDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): PolygonXDifferenceResult =
    jtsGeom.difference(ls.jtsGeom)

  def -(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: OneDimension): OneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: TwoDimensions): TwoDimensionsSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    jtsGeom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def contains(g: Geometry): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: TwoDimensions): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(g: Geometry): Boolean =
    jtsGeom.covers(g.jtsGeom)

  def crosses(g: OneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  def crosses(ps: MultiPoint): Boolean =
    jtsGeom.crosses(ps.jtsGeom)

  def overlaps(g: TwoDimensions): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: TwoDimensions): Boolean =
    jtsGeom.within(g.jtsGeom)

}
