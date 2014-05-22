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

import com.vividsolutions.jts.{geom=>jts}

object MultiLine {
  def apply(ls: Line*): MultiLine = 
    MultiLine(ls)

  def apply(ls: Traversable[Line])(implicit d: DummyImplicit): MultiLine = 
    MultiLine(factory.createMultiLineString(ls.map(_.jtsGeom).toArray))

  implicit def jts2MultiLine(jtsGeom: jts.MultiLineString): MultiLine = apply(jtsGeom)
}

case class MultiLine(jtsGeom: jts.MultiLineString) extends MultiGeometry 
                                                 with Relatable
                                                 with OneDimension {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): MultiLine = { jtsGeom.normalize ; MultiLine(jtsGeom) }

  /** Returns the Lines contained in MultiLine. */
  lazy val lines: Array[Line] = {
    for (i <- 0 until jtsGeom.getNumGeometries) yield {
      Line(jtsGeom.getGeometryN(i).asInstanceOf[jts.LineString])
    }
  }.toArray

  lazy val isClosed: Boolean =
    jtsGeom.isClosed

  lazy val boundary: OneDimensionBoundaryResult =
    jtsGeom.getBoundary

  /** Returns this MulitLine's vertices. */
  lazy val vertices: Array[Point] =
    jtsGeom.getCoordinates.map { c => Point(c.x, c.y) }

  /**
   * Returns the minimum bounding box that contains all the lines in
   * this MultiLine.
   */
  lazy val boundingBox: BoundingBox =
    jtsGeom.getEnvelopeInternal

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(l)
  def intersection(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): OneDimensionAtLeastOneDimensionIntersectionResult =
    p.intersection(this)

  def &(ps: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPoint): MultiPointAtLeastOneDimensionIntersectionResult =
    ps.intersection(this)

  def &(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(ls.jtsGeom)

  def &(ps: MultiPolygon): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(ps.jtsGeom)

  // -- Union

  def |(p: Point): PointMultiLineUnionResult =
    union(p)
  def union(p: Point): PointMultiLineUnionResult =
    p.union(this)

  def |(l: Line): LineOneDimensionUnionResult =
    union(l)
  def union(l:Line): LineOneDimensionUnionResult =
    l.union(this)

  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  def |(ps: MultiPoint): PointMultiLineUnionResult =
    union(ps)
  def union(ps: MultiPoint): PointMultiLineUnionResult =
    jtsGeom.union(ps.jtsGeom)

  def |(ls: MultiLine): LineOneDimensionUnionResult =
    union(ls)
  def union(ls: MultiLine): LineOneDimensionUnionResult =
    jtsGeom.union(ls.jtsGeom)

  def |(ps: MultiPolygon): LineMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): LineMultiPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

  // -- Difference

  def -(p: Point): MultiLinePointDifferenceResult =
    difference(p)
  def difference(p: Point): MultiLinePointDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(l: Line): LineAtLeastOneDimensionDifferenceResult =
    difference(l)
  def difference(l: Line): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(l.jtsGeom)
  
  def -(p: Polygon): LineAtLeastOneDimensionDifferenceResult =
    difference(p)
  def difference(p: Polygon): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(p.jtsGeom)
  
  def -(ps: MultiPoint): MultiLinePointDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): MultiLinePointDifferenceResult = 
    jtsGeom.difference(ps.jtsGeom)
  
  def -(ls: MultiLine): LineAtLeastOneDimensionDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(ls.jtsGeom)
  
  def -(ps: MultiPolygon): LineAtLeastOneDimensionDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): PointMultiLineSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: OneDimension): OneDimensionOneDimensionSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(p: Polygon): OneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)

  def symDifference(ps: MultiPolygon): OneDimensionMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(ps.jtsGeom)

  // -- Predicates

  def contains(g: AtMostOneDimension): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: AtLeastOneDimension): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(g: AtMostOneDimension): Boolean =
    jtsGeom.covers(g.jtsGeom)

  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /** A MultiLine crosses a MultiPoint when it covers
      some points but does not cover others */
  def crosses(ps: MultiPoint): Boolean =
    jtsGeom.crosses(ps.jtsGeom)

  def overlaps(g: OneDimension): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: AtLeastOneDimension): Boolean =
    jtsGeom.within(g.jtsGeom)
}
