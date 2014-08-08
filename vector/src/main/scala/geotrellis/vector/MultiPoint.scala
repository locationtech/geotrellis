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

package geotrellis.vector

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object MultiPoint {
  def apply(ps: Point*): MultiPoint = 
    apply(ps)

  def apply(ps: Traversable[Point]): MultiPoint =
    MultiPoint(factory.createMultiPoint(ps.map(_.jtsGeom).toArray))

  implicit def jts2MultiPoint(jtsGeom: jts.MultiPoint): MultiPoint = apply(jtsGeom)
}

case class MultiPoint(jtsGeom: jts.MultiPoint) extends MultiGeometry 
                                                  with Relatable
                                                  with ZeroDimensions {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): MultiPoint = { jtsGeom.normalize ; MultiPoint(jtsGeom) }

  /** Returns the Points contained in MultiPoint. */
  lazy val points: Array[Point] = {
    for (i <- 0 until jtsGeom.getNumPoints) yield {
      Point(jtsGeom.getGeometryN(i).asInstanceOf[jts.Point])
    }
  }.toArray

  /**
   * Returns the minimum extent that contains all the points
   * of this MultiPoint.
   */
  lazy val envelope: Extent =
    jtsGeom.getEnvelopeInternal

  // -- Intersection

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and p.
   */
  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and p.
   */
  def intersection(p: Point): PointGeometryIntersectionResult =
    jtsGeom.intersection(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and mp.
   */
  def &(mp: MultiPoint): MultiPointMultiPointIntersectionResult =
    intersection(mp)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and mp.
   */
  def intersection(mp: MultiPoint): MultiPointMultiPointIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and g.
   */
  def &(g: AtLeastOneDimension): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this MultiPoint and g.
   */
  def intersection(g: AtLeastOneDimension): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)


  // -- Union


  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and p.
   */
  def |(p: Point): PointZeroDimensionsUnionResult =
    union(p)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and p.
   */
  def union(p: Point): PointZeroDimensionsUnionResult =
    jtsGeom.union(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and l.
   */
  def |(l: Line): ZeroDimensionsLineUnionResult =
    union(l)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and l.
   */
  def union(l: Line): ZeroDimensionsLineUnionResult =
    jtsGeom.union(l.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and p.
   */
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and p.
   */
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and mp.
   */
  def |(mp: MultiPoint): MultiPointMultiPointUnionResult =
    union(mp)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and mp.
   */
  def union(mp: MultiPoint): MultiPointMultiPointUnionResult =
    jtsGeom.union(mp.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and ml.
   */
  def |(ml: MultiLine): MultiPointMultiLineUnionResult =
    union(ml)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and ml.
   */
  def union(ml: MultiLine): MultiPointMultiLineUnionResult =
    jtsGeom.union(ml.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and mp.
   */
  def |(mp: MultiPolygon): MultiPointMultiPolygonUnionResult =
    union(mp)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint and mp.
   */
  def union(mp: MultiPolygon): MultiPointMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)


  // -- Difference


  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in g.
   */
  def -(g: Geometry): MultiPointGeometryDifferenceResult =
    difference(g)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in g.
   */
  def difference(g: Geometry): MultiPointGeometryDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in g and all the points in g that are not in
   * this MultiPoint.
   */
  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiPointSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in l and all the points in l that are not in
   * this MultiPoint.
   */
  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(l.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in p and all the points in p that are not in
   * this MultiPoint.
   */
  def symDifference(p: Polygon): AtMostOneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in ml and all the points in ml that are not in
   * this MultiPoint.
   */
  def symDifference(ml: MultiLine): MultiPointMultiLineSymDifferenceResult =
    jtsGeom.symDifference(ml.jtsGeom)

  /**
   * Computes a Result that represents a Geometry made up of all the points in
   * this MultiPoint that are not in mp and all the points in mp that are not in
   * this MultiPoint.
   */
  def symDifference(mp: MultiPolygon): MultiPointMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)


  // -- Misc.


  /**
   * Computes the smallest convex Polygon that contains all the points in the
   * MultiPoint. Applies only to MultiPoints with three or more points.
   *
   * TODO: Assert that the MultiPoint has at least 3 points. Investigate the
   * case where given 3 points that form a straight line, convexHull() returns
   * a line instead of a polygon.
   */
  def convexHull(): Polygon =
    jtsGeom.convexHull() match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for MultiPoint convexHull: ${x.getGeometryType}")
    }


  // -- Predicates


  /**
   * Tests whether this MultiPoint contains the specified ZeroDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF*.
   */
  def contains(g: ZeroDimensions): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /**
   * Tests whether this MultiPoint is covered by the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*F**F*** or *TF**F*** or **FT*F*** or **F*TF***.
   */
  def coveredBy(g: Geometry): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /**
   * Tests whether this MultiPoint covers the specified ZeroMostOneDimensions g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
   */
  def covers(g: ZeroDimensions): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /**
    * Tests whether this MultiPoint crosses the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*T****** (P/L and P/A).
    */
  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /**
   * Tests whether this MultiPoint overlaps the specified MultiPoint mp.
   * Returns true if The DE-9IM Intersection Matrix for the two MultiPoints is
   * T*T***T**.
   */
  def overlaps(mp: MultiPoint): Boolean =
    jtsGeom.overlaps(mp.jtsGeom)

  /**
   * Tests whether this MultiPoint touches the specified AtLeastOneDimension g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * FT*******, F**T***** or F***T****.
   */
  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /**
   * Tests whether this MultiPoint is within the specified Geometry g.
   * Returns true if the DE-9IM Intersection Matrix for the two geometries is
   * T*F**F***.
   */
  def within(g: Geometry): Boolean =
    jtsGeom.within(g.jtsGeom)
}
