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
import geotrellis.vector.GeomFactory._
import spire.syntax.cfor._

/** Companion object to [[MultiLine]] */
object MultiLine {
  lazy val EMPTY = MultiLine(Seq[Line]())

  def apply(ls: Line*): MultiLine =
    MultiLine(ls)

  def apply(ls: Traversable[Line]): MultiLine =
    MultiLine(factory.createMultiLineString(ls.map(_.jtsGeom).toArray))

  def apply(ls: Array[Line]): MultiLine = {
    val len = ls.length
    val arr = Array.ofDim[jts.LineString](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ls(i).jtsGeom
    }

    MultiLine(factory.createMultiLineString(arr))
  }

  implicit def jts2MultiLine(jtsGeom: jts.MultiLineString): MultiLine = apply(jtsGeom)
}

/** Class representing a geometry of multiple lines */
case class MultiLine(jtsGeom: jts.MultiLineString) extends MultiGeometry
                                                      with OneDimension {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): MultiLine = {
    val geom = jtsGeom.clone.asInstanceOf[jts.MultiLineString]
    geom.normalize
    MultiLine(geom)
  }

  /** Returns the Lines contained in this MultiLine. */
  lazy val lines: Array[Line] = {
    for (i <- 0 until jtsGeom.getNumGeometries) yield {
      Line(jtsGeom.getGeometryN(i).clone.asInstanceOf[jts.LineString])
    }
  }.toArray

  /** Tests if the initial vertex equals the final vertex for every Line in this MultiLine. */
  lazy val isClosed: Boolean =
    jtsGeom.isClosed

  /** Returns the boundary of this MultiLine.
    * The boundary of a non-closed MultiLine consists of all the end points of
    * the non-closed lines that make up the MultiLine. The boundary of a closed
    * MultiLine is empty.
    */
  lazy val boundary: OneDimensionBoundaryResult =
    jtsGeom.getBoundary

  // -- Intersection

  /** Computes a Result that represents a Geometry made up of the points shared
    * by the contained lines.
    */
  def intersection(): MultiLineMultiLineIntersectionResult =
    lines.map(_.jtsGeom).reduce[jts.Geometry] {
      _.intersection(_)
    }

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and p.
    */
  def &(p: Point): PointOrNoResult =
    intersection(p)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and p.
    */
  def intersection(p: Point): PointOrNoResult =
    jtsGeom.intersection(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and g.
    */
  def &(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and g.
    */
  def intersection(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and mp.
    */
  def &(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(mp)

  /** Computes a Result that represents a Geometry made up of the points shared
    * by this MultiLine and mp.
    */
  def intersection(mp: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)


  // -- Union

  /** Computes the union of contained lines.
    * Useful for merging overlapping line segments.
    */
  def union(): MultiLineMultiLineUnionResult =
    jtsGeom.union

  /** Computes a Result that represents a Geometry made up of all the points in
     * this MultiLine and p.
     */
  def |(p: Point): PointMultiLineUnionResult =
    union(p)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and p.
    */
  def union(p: Point): PointMultiLineUnionResult =
    jtsGeom.union(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and l.
    */
  def |(l: Line): LineOneDimensionUnionResult =
    union(l)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and l.
    */
  def union(l:Line): LineOneDimensionUnionResult =
    jtsGeom.union(l.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and p.
    */
  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and p.
    */
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and mp.
    */
  def |(mp: MultiPoint): MultiPointMultiLineUnionResult =
    union(mp)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and mp.
    */
  def union(mp: MultiPoint): MultiPointMultiLineUnionResult =
    jtsGeom.union(mp.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and ml.
    */
  def |(ml: MultiLine): MultiLineMultiLineUnionResult =
    union(ml)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and ml.
    */
  def union(ml: MultiLine): MultiLineMultiLineUnionResult =
    jtsGeom.union(ml.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and mp.
    */
  def |(mp: MultiPolygon): MultiLineMultiPolygonUnionResult =
    union(mp)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine and mp.
    */
  def union(mp: MultiPolygon): MultiLineMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)


  // -- Difference

  /** Computes a Result that represents a Geometry made up of all the points in
    * the first line that are not in the other contained lines.
    */
  def difference(): MultiLineMultiLineDifferenceResult =
    lines.map(_.jtsGeom).reduce[jts.Geometry] {
      _.difference(_)
    }

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in g.
    */
  def -(g: Geometry): MultiLineGeometryDifferenceResult =
    difference(g)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in g.
    */
  def difference(g: Geometry): MultiLineGeometryDifferenceResult =
    jtsGeom.difference(g.jtsGeom)


  // -- SymDifference

  /** Computes a Result that represents a Geometry made up of all the points in
    * the contained lines that are unique to one line.
    */
  def symDifference(): MultiLineMultiLineSymDifferenceResult =
    lines.map(_.jtsGeom).reduce[jts.Geometry] {
      _.symDifference(_)
    }

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in p and the point p if it is not in this
    * MultiLine.
    */
  def symDifference(p: Point): PointMultiLineSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in mp and all the points in mp that are not in
    * this MultiLine.
    */
  def symDifference(mp: MultiPoint): MultiPointMultiLineSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)

  /**
    * Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in g and all the points in g that are not in
    * this MultiLine.
    */
  def symDifference(g: OneDimension): OneDimensionOneDimensionSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in p and all the points in p that are not in
    * this MultiLine.
    */
  def symDifference(p: Polygon): AtMostOneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  /** Computes a Result that represents a Geometry made up of all the points in
    * this MultiLine that are not in mp and all the points in mp that are not in
    * this MultiLine.
    */
  def symDifference(mp: MultiPolygon): MultiLineMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)


  // -- Predicates

  /**
    * Tests whether this MultiLine contains the specified AtMostOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****FF*.
    */
  def contains(g: AtMostOneDimension): Boolean =
    jtsGeom.contains(g.jtsGeom)

  /** Tests whether this MultiLine is covered by the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is T*F**F*** or
    * *TF**F*** or **FT*F*** or **F*TF***.
    */
  def coveredBy(g: AtLeastOneDimension): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  /** Tests whether this MultiLine covers the specified AtMostOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****FF* or *T****FF* or ***T**FF* or ****T*FF*.
    */
  def covers(g: AtMostOneDimension): Boolean =
    jtsGeom.covers(g.jtsGeom)

  /** Tests whether this MultiLine crosses the specified MultiPoint mp.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*****T** (L/P).
    */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  /** Tests whether this MultiLine crosses the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * 0******** (L/L) or T*****T** (L/P and L/A).
    */
  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /** Tests whether this MultiLine overlaps the specified OneDimension g.
    * Returns true if The DE-9IM Intersection Matrix for the two geometries is
    * 1*T***T**.
    */
  def overlaps(g: OneDimension): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  /** Tests whether this MultiLine touches the specified Geometry g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * FT*******, F**T***** or F***T****.
    */
  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  /** Tests whether this MultiLine is within the specified AtLeastOneDimension g.
    * Returns true if the DE-9IM Intersection Matrix for the two geometries is
    * T*F**F***.
    */
  def within(g: AtLeastOneDimension): Boolean =
    jtsGeom.within(g.jtsGeom)
}
