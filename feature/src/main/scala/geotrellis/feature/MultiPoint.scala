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

  implicit def jts2MultiPoint(jtsGeom: jts.MultiPoint): MultiPoint = apply(jtsGeom)
}

case class MultiPoint(jtsGeom: jts.MultiPoint) extends MultiGeometry 
                                             with Relatable
                                             with ZeroDimensions {

  /** Returns the Points contained in MultiPoint. */
  lazy val points: Array[Point] = {
    for (i <- 0 until jtsGeom.getNumPoints) yield {
      Point(jtsGeom.getGeometryN(i).asInstanceOf[jts.Point])
    }
  }.toArray

  // -- Intersection

  def &(p: Point): PointOrNoResult =
    intersection(p)
  def intersection(p: Point): PointOrNoResult =
    p.intersection(this)

  def &(l: Line): MultiPointGeometryIntersectionResult =
    intersection(l)
  def intersection(l: Line): MultiPointGeometryIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): MultiPointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): MultiPointGeometryIntersectionResult =
    p.intersection(this)

  def &(ps: MultiPoint): MultiPointGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPoint): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  def &(ls: MultiLine): MultiPointGeometryIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(ls.jtsGeom)

  def &(ps: MultiPolygon): MultiPointGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  // -- Union

  def |(p: Point): PointZeroDimensionsUnionResult =
    union(p)
  def union(p: Point): PointZeroDimensionsUnionResult =
    p.union(this)

  def |(l: Line): ZeroDimensionsLineUnionResult =
    union(l)
  def union(l:Line): ZeroDimensionsLineUnionResult =
    l.union(this)

  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    p.union(this)

  def |(ps: MultiPoint): PointZeroDimensionsUnionResult =
    union(ps)
  def union(ps: MultiPoint): PointZeroDimensionsUnionResult =
    jtsGeom.union(ps.jtsGeom)

  def |(ls: MultiLine): PointMultiLineUnionResult =
    union(ls)
  def union(ls: MultiLine): PointMultiLineUnionResult =
    jtsGeom.union(ls.jtsGeom)

  def |(ps: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

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
