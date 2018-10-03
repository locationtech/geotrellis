/*
 * Copyright 2016 Azavea
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

package geotrellis.vector

import org.locationtech.jts.{geom => jts}
import GeomFactory._
import org.locationtech.jts.geom.CoordinateSequence
import spire.syntax.cfor._

object Line {

  implicit def jtsToLine(jtsGeom: jts.LineString): Line =
    apply(jtsGeom)

  def apply(points: (Double, Double)*)(implicit d: DummyImplicit): Line =
    apply(points)

  def apply(points: Traversable[(Double, Double)])(implicit d: DummyImplicit): Line =
    apply(points.map { case (x,y) => Point(x,y) })

  def apply(points: Point*): Line =
    apply(points.toList)

  def apply(coords: CoordinateSequence): Line =
    Line(factory.createLineString(coords))

  def apply(points: Traversable[Point]): Line = {
    if (points.size < 2) {
      sys.error("Invalid line: Requires 2 or more points.")
    }

    val pointArray = points.toArray
    val coords = Array.ofDim[jts.Coordinate](pointArray.size)
    cfor(0)(_ < pointArray.size, _ + 1) { i =>
      coords(i) = pointArray(i).jtsGeom.getCoordinate
    }

    Line(factory.createLineString(coords))
  }
}

case class Line(jtsGeom: jts.LineString) extends Geometry
                                            with OneDimension {

  assert(!jtsGeom.isEmpty, s"LineString Empty: $jtsGeom")

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): Line = {
    val geom = jtsGeom.clone.asInstanceOf[jts.LineString]
    geom.normalize
    Line(geom)
  }

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

  /* The first [[Point]] in this Line, which we know to exist. */
  def head: Point = jtsGeom.getStartPoint

  /* The last [[Point]] in this Line, which we know to exist. */
  def last: Point = jtsGeom.getEndPoint

  /** Returns the points which determine this line (i.e. its vertices */
  def points: Array[Point] = vertices

  /** Returns the points which determine this line (i.e. its vertices */
  override lazy val vertices: Array[Point] = {
    val arr = Array.ofDim[Point](jtsGeom.getNumPoints)
    val sequence = jtsGeom.getCoordinateSequence

    cfor(0)(_ < arr.length, _ + 1) { i =>
      arr(i) = Point(sequence.getX(i), sequence.getY(i))
    }

    arr
  }

  /** Returns the length of this Line. */
  lazy val length: Double =
    jtsGeom.getLength

  /** Returns a closed version of the line. If already closed, just return this. */
  def closed(): Line =
    if(isClosed) this
    else {
      Line(vertices :+ vertices.head)
    }

  // -- Intersection

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and p.
    * @param p  the point to intersect with
    */
  def &(p: Point): PointOrNoResult =
    intersection(p)

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and a Point.
    * @param p  the point to intersect with
    */
  def intersection(p: Point): PointOrNoResult =
    jtsGeom.intersection(p.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and a MultiPoint.
    * @param mp  the multipoint to intersect with
    */
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(mp)

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and a MultiPoint.
    * @param mp  the multipoint to intersect with
    */
  def intersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and a geometry of at least one dimension.
    * @param g  the geometry to intersect with
    */
  def &(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /**
    * Computes a Result that represents a Geometry made up of the points shared
    * by this Line and g.
    * @param g  the geometry to intersect with
    */
  def intersection(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  // -- Union

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a a ZeroDimensions geometry.
    * @param g  the geometry to union with
    */
  def |(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    union(g)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a a ZeroDimensions geometry.
    * @param g  the geometry to union with
    */
  def union(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a OneDimension geometry.
    * @param g  the geometry to union with
    */
  def |(g: OneDimension): LineOneDimensionUnionResult =
    union(g)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a OneDimension geometry.
    * @param g  the geometry to union with
    */
  def union(g: OneDimension): LineOneDimensionUnionResult =
    jtsGeom.union(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a Polygon.
    * @param p  the geometry to union with
    */
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a Polygon.
    * @param p  the geometry to union with
    */
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a MultiPolygon.
    * @param p  the geometry to union with
    */
  def |(mp: MultiPolygon): LineMultiPolygonUnionResult =
    union(mp)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line and a MultiPolygon.
    * @param p  the geometry to union with
    */
  def union(mp: MultiPolygon): LineMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)

  // -- Difference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   * @param g  the geometry to diff against
   */
  def -(g: ZeroDimensions): LineResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   * @param g  the geometry to diff against
   */
  def difference(g: ZeroDimensions): LineResult =
    jtsGeom.difference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this Line that are not in g.
   * @param g  the geometry to diff against
   */
  def -(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    difference(g)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line that are not in g.
   * @param g  the geometry to diff against
    */
  def difference(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line that are not in g and all the points in g that are not in
    * this Line.
   * @param g  the geometry to diff against
    */
  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line that are not in g and all the points in g that are not in
    * this Line.
   * @param g  the geometry to diff against
    */
  def symDifference(g: OneDimension): OneDimensionOneDimensionSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line that are not in p and all the points in p that are not in
    * this Line.
   * @param p  the polygon to diff against
    */
  def symDifference(p: Polygon): AtMostOneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this Line that are not in mp and all the points in mp that are not in
    * this Line.
   * @param mp  the multipolygon to diff against
    */
  def symDifference(mp: MultiPolygon): LineMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)


  // -- Buffer


  /**
    * Computes a buffer area around this Line having width d.
    * @param d  the buffer width
    */
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
    * @param g  the geometry to use in containment check
    */
  def contains(g: AtMostOneDimension): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /**
    * Tests whether this Line is covered by the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is T*F**F*** or
    * *TF**F*** or **FT*F*** or **F*TF***.
    * @param g  the geometry to use in checking whether this line is covered
    */
  def coveredBy(g: AtLeastOneDimension): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /**
    * Tests whether this Line covers the specified AtMostOneDimensions g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
    * @param g  the geometry to check for coverage over
    */
  def covers(g: AtMostOneDimension): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /**
    * Tests whether this Line crosses the specified MultiPoint mp.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****T** (L/P).
    * @param mp the multipoint to check if this line crosses
    */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  /**
    * Tests whether this Line crosses the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * 0******** (L/L) or T*T****** (L/A).
    * @param g the geometry to check if this line crosses
    */
  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /**
    * Tests whether this Line overlaps the specified OneDimension g.
    * Returns true if The DE-9IM Intersection Matrix for the two geometries is
    * 1*T***T**.
    * @param g  the geometry to check for overlap against
    */
  def overlaps(g: OneDimension): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  /**
    * Tests whether this Line touches the specified Geometry g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * FT*******, F**T***** or F***T****.
    * @param g  the geometry to check if this line is touching
    */
  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /**
    * Tests whether this Line is within the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*F**F***.
    * @param g  the geometry to check if this line is within
    */
  def within(g: AtLeastOneDimension): Boolean =
    jtsGeom.within(g.jtsGeom)
}
