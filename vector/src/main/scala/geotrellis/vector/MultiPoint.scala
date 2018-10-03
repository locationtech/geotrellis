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

import GeomFactory._

import org.locationtech.jts.{geom => jts}

import spire.syntax.cfor._

object MultiPoint {
  lazy val EMPTY = MultiPoint(Seq[Point]())

  def apply(ps: Point*): MultiPoint =
    apply(ps)

  def apply(ps: Traversable[Point]): MultiPoint =
    MultiPoint(factory.createMultiPoint(ps.map(_.jtsGeom).toArray))

  def apply(ps: Array[Point]): MultiPoint = {
    val len = ps.length
    val arr = Array.ofDim[jts.Point](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ps(i).jtsGeom
    }

    MultiPoint(factory.createMultiPoint(arr))
  }

  def apply(ps: Traversable[(Double, Double)])(implicit d: DummyImplicit): MultiPoint =
    MultiPoint(factory.createMultiPoint(ps.map { p => new jts.Coordinate(p._1, p._2) }.toArray))

  implicit def jts2MultiPoint(jtsGeom: jts.MultiPoint): MultiPoint = apply(jtsGeom)
}

/** Class representing a geometry of multiple points */
case class MultiPoint(jtsGeom: jts.MultiPoint) extends MultiGeometry
                                                  with ZeroDimensions {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): MultiPoint = {
    val geom = jtsGeom.clone.asInstanceOf[jts.MultiPoint]
    geom.normalize
    MultiPoint(geom)
  }

  /** Returns the Points contained in MultiPoint. */
  lazy val points: Array[Point] = vertices

  // -- Intersection

  /** Computes a Result that represents a Geometry made up of the points shared
    * by the contained lines.
    */
  def &(): MultiPointMultiPointIntersectionResult =
    intersection()

  def intersection(): MultiPointMultiPointIntersectionResult =
    points.map(_.jtsGeom).reduce[jts.Geometry] {
      _.intersection(_)
    }

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and p.
    */
  def &(p: Point): PointOrNoResult =
    intersection(p)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and p.
    */
  def intersection(p: Point): PointOrNoResult =
    jtsGeom.intersection(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and mp.
    */
  def &(mp: MultiPoint): MultiPointMultiPointIntersectionResult =
    intersection(mp)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and mp.
    */
  def intersection(mp: MultiPoint): MultiPointMultiPointIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and g.
    */
  def &(g: AtLeastOneDimension): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiPoint and g.
    */
  def intersection(g: AtLeastOneDimension): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  // -- Union

  /** Computes the union of the contained points.
    * Useful for de-duplication.
    */
  def union(): MultiPointMultiPointUnionResult =
    jtsGeom.union

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and p.
    */
  def |(p: Point): PointZeroDimensionsUnionResult =
    union(p)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and p.
    */
  def union(p: Point): PointZeroDimensionsUnionResult =
    jtsGeom.union(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and l.
    */
  def |(l: Line): ZeroDimensionsLineUnionResult =
    union(l)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and l.
    */
  def union(l: Line): ZeroDimensionsLineUnionResult =
    jtsGeom.union(l.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and p.
    */
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and p.
    */
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and mp.
    */
  def |(mp: MultiPoint): MultiPointMultiPointUnionResult =
    union(mp)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and mp.
    */
  def union(mp: MultiPoint): MultiPointMultiPointUnionResult =
    jtsGeom.union(mp.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and ml.
    */
  def |(ml: MultiLine): MultiPointMultiLineUnionResult =
    union(ml)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and ml.
    */
  def union(ml: MultiLine): MultiPointMultiLineUnionResult =
    jtsGeom.union(ml.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and mp.
    */
  def |(mp: MultiPolygon): MultiPointMultiPolygonUnionResult =
    union(mp)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint and mp.
    */
  def union(mp: MultiPolygon): MultiPointMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)

  // -- Difference

  /** Computes a Result that represents a Geometry made up of all the points in
    * the first line not in the other contained lines.
    */
  def difference(): MultiPointMultiPointDifferenceResult =
    points.map(_.jtsGeom).reduce[jts.Geometry] {
      _.difference(_)
    }

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint that are not in g.
    */
  def -(g: Geometry): MultiPointGeometryDifferenceResult =
    difference(g)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint that are not in g.
    */
  def difference(g: Geometry): MultiPointGeometryDifferenceResult =
    jtsGeom.difference(g.jtsGeom)

  // -- SymDifference

  /** Computes a Result that represents a Geometry made up of all the unique
    * points in this MultiPoint.
    */
  def symDifference(): MultiPointMultiPointSymDifferenceResult =
    points.map(_.jtsGeom).reduce[jts.Geometry] {
      _.symDifference(_)
    }

  /** Computes a Result that represents a Geometry made up of all the points in
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

  /** Computes a Result that represents a Geometry made up of all the points in
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

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiPoint that are not in mp and all the points in mp that are not in
    * this MultiPoint.
    */
  def symDifference(mp: MultiPolygon): MultiPointMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)

  // -- Predicates

  /** Tests whether this MultiPoint contains the specified ZeroDimensions g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****FF*.
    */
  def contains(g: ZeroDimensions): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /** Tests whether this MultiPoint is covered by the specified Geometry g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*F**F*** or *TF**F*** or **FT*F*** or **F*TF***.
    */
  def coveredBy(g: Geometry): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /** Tests whether this MultiPoint covers the specified ZeroMostOneDimensions g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
    */
  def covers(g: ZeroDimensions): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /** Tests whether this MultiPoint crosses the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*T****** (P/L and P/A).
    */
  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /** Tests whether this MultiPoint overlaps the specified MultiPoint mp.
    * Returns true if The DE-9IM Intersection Matrix for the two MultiPoints is
    * T*T***T**.
    */
  def overlaps(mp: MultiPoint): Boolean =
    jtsGeom.overlaps(mp.jtsGeom)

  /** Tests whether this MultiPoint touches the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * FT*******, F**T***** or F***T****.
    */
  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /** Tests whether this MultiPoint is within the specified Geometry g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*F**F***.
    */
  def within(g: Geometry): Boolean =
    jtsGeom.within(g.jtsGeom)
}
