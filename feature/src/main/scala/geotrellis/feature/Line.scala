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

object Line {

  implicit def jtsToLine(jtsGeom: jts.LineString): Line =
    apply(jtsGeom)

  def apply(points: Point*): Line =
    apply(points.toList)

  def apply(points: Seq[Point])(implicit d: DummyImplicit): Line =
    apply(points.toList)

  def apply(points: Array[Point]): Line =
    apply(points.toList)

  def apply(points: List[Point]): Line = {
    if (points.length < 2) {
      sys.error("Invalid line: Requires 2 or more points.")
    }

    Line(factory.createLineString(points.map(_.jtsGeom.getCoordinate).toArray))
  }

}

case class Line(jtsGeom: jts.LineString) extends Geometry
                                         with Relatable
                                         with OneDimension {

  assert(!jtsGeom.isEmpty)
  assert(jtsGeom.isValid)

  lazy val points: List[Point] = jtsGeom.getCoordinates.map(c => Point(c.x, c.y)).toList

  lazy val isClosed: Boolean =
    jtsGeom.isClosed

  lazy val isSimple: Boolean =
    jtsGeom.isSimple

  lazy val boundary: OneDimensionBoundaryResult =
    jtsGeom.getBoundary

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
        sys.error(s"Unexpected result for Line boundingBox: ${x.getGeometryType}")
    }

  lazy val length: Double =
    jtsGeom.getLength

  // -- Intersection

  def &(p: Point): PointOrNoResult =
    intersection(p)
  def intersection(p: Point): PointOrNoResult =
    p.intersection(this)

  def &(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(g)
  def intersection(g: AtLeastOneDimension): OneDimensionAtLeastOneDimensionIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)

  def &(mp: MultiPoint): MultiPointGeometryIntersectionResult =
    intersection(mp)
  def intersection(mp: MultiPoint): MultiPointGeometryIntersectionResult =
    jtsGeom.intersection(mp.jtsGeom)

  // -- Union

  def |(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    union(g)
  def union(g: ZeroDimensions): ZeroDimensionsLineUnionResult =
    jtsGeom.union(g.jtsGeom)

  def |(g: OneDimension): LineOneDimensionUnionResult =
    union(g)
  def union(g: OneDimension): LineOneDimensionUnionResult =
    jtsGeom.union(g.jtsGeom)

  def |(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  def |(mp: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    union(mp)
  def union(mp: MultiPolygon): AtMostOneDimensionMultiPolygonUnionResult =
    jtsGeom.union(mp.jtsGeom)

  // -- Difference

  def -(g: ZeroDimensions): LineResult =
    difference(g)
  def difference(g: ZeroDimensions): LineResult =
    jtsGeom.difference(g.jtsGeom)

  def -(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    difference(g)
  def difference(g: AtLeastOneDimension): LineAtLeastOneDimensionDifferenceResult =
    jtsGeom.difference(g.jtsGeom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: OneDimension): OneDimensionOneDimensionSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(p: Polygon): OneDimensionPolygonSymDifferenceResult =
    jtsGeom.symDifference(p.jtsGeom)
  
  def symDifference(mp: MultiPolygon): OneDimensionMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(mp.jtsGeom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    jtsGeom.buffer(d) match {
      case p: jts.Polygon => Polygon(p)
      case x =>
        sys.error(s"Unexpected result for Point buffer: ${x.getGeometryType}")
    }

  // -- Predicates

  def contains(g: AtMostOneDimension): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: AtLeastOneDimension): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(g: AtMostOneDimension): Boolean =
    jtsGeom.covers(g.jtsGeom)

  def crosses(g: AtLeastOneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  /** A Line crosses a MultiPoint when it covers
      some points but does not cover others */
  def crosses(mp: MultiPoint): Boolean =
    jtsGeom.crosses(mp.jtsGeom)

  def overlaps(g: OneDimension): Boolean =
    jtsGeom.overlaps(g.jtsGeom)

  def touches(g: Geometry): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: AtLeastOneDimension): Boolean =
    jtsGeom.within(g.jtsGeom)

}
