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


  /** Tests whether this Polygon is a rectangle. */
  lazy val isRectangle: Boolean =
    jtsGeom.isRectangle

  /** Returns the area of this Polygon. */
  lazy val area: Double =
    jtsGeom.getArea

  /** Returns the exterior ring of this Polygon. */
  lazy val exterior: Line =
    Line(jtsGeom.getExteriorRing)

  /**
   * Returns the boundary of this Polygon.
   * The boundary of a Polygon is the set of closed curves corresponding to its
   * exterior and interior boundaries.
   */
  lazy val boundary: PolygonBoundaryResult =
    jtsGeom.getBoundary

  /** Returns this Polygon's vertices. */
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


  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and p.
   */
  def &(p: Point): PointOrNoResult =
    intersection(p)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and p.
   */
  def intersection(p: Point): PointOrNoResult =
    jtsGeom.intersection(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and mp.
   */
  def &(mp: MultiPoint): MultiPointGeometryIntersectionResult =
    intersection(mp)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and mp.
   */
  def intersection(mp: MultiPoint): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def &(g: OneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def intersection(g: OneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def &(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def intersection(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)


  // -- Union

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def |(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def union(g: AtMostOneDimension): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def |(g: TwoDimensions): TwoDimensionsTwoDimensionsUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon and g.
   */
  def union(g: TwoDimensions): TwoDimensionsTwoDimensionsUnionResult =
    jtsGeom.union(g.jtsGeom)


  // -- Difference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def -(g: AtMostOneDimension): PolygonAtMostOneDimensionDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def difference(g: AtMostOneDimension): PolygonAtMostOneDimensionDifferenceResult =
    jtsGeom.difference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def -(g: TwoDimensions): TwoDimensionsTwoDimensionsDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g.
   */
  def difference(g: TwoDimensions): TwoDimensionsTwoDimensionsDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g and all the points in g that are not in
   * this Polygon.
   */
  def symDifference(g: ZeroDimensions): ZeroDimensionsPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g and all the points in g that are not in
   * this Polygon.
   */
  def symDifference(g: OneDimension): OneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Polygon that are not in g and all the points in g that are not in
   * this Polygon.
   */
  def symDifference(g: TwoDimensions): TwoDimensionsTwoDimensionsSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)


  // -- Buffer


  /** Computes a buffer area around this Polygon having width d. */
  def buffer(d: Double): Polygon =
    jtsGeom.buffer(d) match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for Polygon buffer: ${x.getGeometryType}")
    }

  // -- Predicates


  /**
   * Tests whether this Polygon contains the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF*.
   */
  def contains(g: Geometry): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /**
   * Tests whether this Polygon is covered by the specified TwoDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is T*F**F*** or
   * *TF**F*** or **FT*F*** or **F*TF***.
   */
  def coveredBy(g: TwoDimensions): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /**
   * Tests whether this Polygon covers the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
   */
  def covers(g: Geometry): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /**
   * Tests whether this Polygon crosses the specified MultiPoint mp.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****T** (A/P).
   */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  /**
   * Tests whether this Polygon crosses the specified OneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****T** (A/L).
   */
  def crosses(g: OneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /**
   * Tests whether this Polygon overlaps the specified TwoDimensions g.
   * Returns true if The DE-9IM Intersection Matrix for the two geometries is
   * T*T***T**.
   */
  def overlaps(g: TwoDimensions): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  /**
   * Tests whether this Polygon touches the specified geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * FT*******, F**T***** or F***T****.
   */
  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /**
   * Tests whether this Polygon is within the specified TwoDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*F**F***.
   */
  def within(g: TwoDimensions): Boolean =
    jtsGeom.within(g.jtsGeom)

}
