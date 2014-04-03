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

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object MultiPoint {
  def apply(ps: Point*): MultiPoint = 
    apply(ps)

  def apply(ps: Traversable[Point]): MultiPoint =
    MultiPoint(factory.createMultiPoint(ps.map(_.jtsGeom).toArray))
}

case class MultiPoint(jtsGeom: jts.MultiPoint) extends MultiGeometry 
                                             with Relatable
                                             with ZeroDimensions {

  assert(jtsGeom.isValid)


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


  def -(other: Geometry): MultiPointDifferenceResult =
    difference(other)
  def difference(other: Geometry): MultiPointDifferenceResult =
    jtsGeom.difference(other.jtsGeom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiPointSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(l.jtsGeom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  def symDifference(ls: MultiLine): ZeroDimensionsMultiLineSymDifferenceResult =
    jtsGeom.symDifference(ls.jtsGeom)

  def symDifference(ps: MultiPolygon): ZeroDimensionsMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(ps.jtsGeom)
                         
  // -- Misc.

  def convexHull: Polygon =
    jtsGeom.convexHull.asInstanceOf[jts.Polygon]

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: Geometry): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(p: ZeroDimensions): Boolean =
    jtsGeom.covers(p.jtsGeom)

  def overlaps(ps: MultiPoint): Boolean =
    jtsGeom.overlaps(ps.jtsGeom)

  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: Geometry): Boolean =
    jtsGeom.within(g.jtsGeom)

}
