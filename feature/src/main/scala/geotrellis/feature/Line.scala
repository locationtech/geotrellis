/*
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
 */

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Line {

  implicit def jtsToLine(jtsGeom: jts.LineString): Line =
    apply(jtsGeom)

  def apply(points: (Double, Double)*)(implicit d: DummyImplicit): Line =
    apply(points)

  def apply(points: Traversable[(Double, Double)])(implicit d: DummyImplicit): Line =
    apply(points.map { case (x,y) => Point(x,y) })

  def apply(points: Point*): Line =
    apply(points.toList)

  def apply(points: Traversable[Point]): Line = {
    if (points.size < 2) {
      sys.error("Invalid line: Requires 2 or more points.")
    }

    Line(factory.createLineString(points.map(_.jtsGeom.getCoordinate).toArray))
  }
}

case class Line(jtsGeom: jts.LineString) extends Geometry
                                            with Relatable
                                            with OneDimension {

  assert(!jtsGeom.isEmpty, s"LineString Empty: $jtsGeom")

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): Line = { jtsGeom.normalize ; Line(jtsGeom) }

  /** Returns this Line's vertices as a list of Points. */
  lazy val points: Array[Point] =
    jtsGeom.getCoordinates.map(c => Point(c.x, c.y))

  /** Tests if the initial vertex equals the final vertex. */
  lazy val isClosed: Boolean =
    jtsGeom.isClosed

  /**
   * Tests whether this Line is simple.
   * A Line is simple iff it does not self-intersect at points other than
   * boundary points.
   */
  lazy val isSimple: Boolean =
    jtsGeom.isSimple

  /**
   * Returns the boundary of this Line.
   * The boundary of a non-closed Line consists of its two end points. The
   * boundary of a closed Line is empty.
   */
  lazy val boundary: OneDimensionBoundaryResult =
    jtsGeom.getBoundary

  /** Returns this Line's vertices. */
  lazy val vertices: Array[Point] =
    jtsGeom.getCoordinates.map { c => Point(c.x, c.y) }

  /**
   * Returns the minimum extent that contains this Line.
   */
  lazy val envelope: Extent =
    jtsGeom.getEnvelopeInternal

  /** Returns the length of this Line. */
  lazy val length: Double =
    jtsGeom.getLength

  /** Returns a closed version of the line. If already closed, just return this. */
  def closed(): Line =
    if(isClosed) this
    else {
      Line(points :+ points.head)
    }

  // -- Intersection

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and p.
   */
  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and p.
   */
  def intersection(p: Point): PointGeometryIntersectionResult =
    jtsGeom.intersection(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and g.
   */
  def &(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and g.
   */
  def intersection(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and mp.
   */
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(mp)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Line and mp.
   */
  def intersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)


  // -- Union

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and g.
   */
  def |(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and g.
   */
  def union(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and g.
   */
  def |(g: OneDimension): LineOneDimensionUnionResult =
    union(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and g.
   */
  def union(g: OneDimension): LineOneDimensionUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and p.
   */
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and p.
   */
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and mp.
   */
  def |(mp: MultiPolygon): LineMultiPolygonUnionResult =
    union(mp)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line and mp.
   */
  def union(mp: MultiPolygon): LineMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)

  // -- Difference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   */
  def -(g: ZeroDimensions): LineResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   */
  def difference(g: ZeroDimensions): LineResult =
    jtsGeom.difference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   */
  def -(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   */
  def difference(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g and all the points in g that are not in
   * this Line.
   */
  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g and all the points in g that are not in
   * this Line.
   */
  def symDifference(g: OneDimension): OneDimensionOneDimensionSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in p and all the points in p that are not in
   * this Line.
   */
  def symDifference(p: Polygon): AtMostOneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in mp and all the points in mp that are not in
   * this Line.
   */
  def symDifference(mp: MultiPolygon): LineMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)


  // -- Buffer


  /** Computes a buffer area around this Line having width d. */
  def buffer(d: Double): Polygon =
    jtsGeom.buffer(d) match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for Line buffer: ${x.getGeometryType}")
    }


  // -- Predicates

  /**
   * Tests whether this Line contains the specified AtMostOneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF*.
   */
  def contains(g: AtMostOneDimension): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /**
   * Tests whether this Line is covered by the specified AtLeastOneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is T*F**F*** or
   * *TF**F*** or **FT*F*** or **F*TF***.
   */
  def coveredBy(g: AtLeastOneDimension): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /**
   * Tests whether this Line covers the specified AtMostOneDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
   */
  def covers(g: AtMostOneDimension): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /**
   * Tests whether this Line crosses the specified MultiPoint mp.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****T** (L/P).
   */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  /**
   * Tests whether this Line crosses the specified AtLeastOneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * 0******** (L/L) or T*T****** (L/A).
   */
  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /**
   * Tests whether this Line overlaps the specified OneDimension g.
   * Returns true if The DE-9IM Intersection Matrix for the two geometries is
   * 1*T***T**.
   */
  def overlaps(g: OneDimension): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  /**
   * Tests whether this Line touches the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * FT*******, F**T***** or F***T****.
   */
  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /**
   * Tests whether this Line is within the specified AtLeastOneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*F**F***.
   */
  def within(g: AtLeastOneDimension): Boolean =
    jtsGeom.within(g.jtsGeom)
}
