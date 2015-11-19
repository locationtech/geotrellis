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
import com.vividsolutions.jts.geom.TopologyException

import com.vividsolutions.jts.{geom => jts}

import spire.syntax.cfor._

object MultiPolygon {
  lazy val EMPTY = MultiPolygon(Seq[Polygon]())

  def apply(ps: Polygon*): MultiPolygon =
    apply(ps)

  def apply(ps: Traversable[Polygon]): MultiPolygon =
    MultiPolygon(factory.createMultiPolygon(ps.map(_.jtsGeom).toArray))

  def apply(ps: Array[Polygon]): MultiPolygon = {
    val len = ps.length
    val arr = Array.ofDim[jts.Polygon](len)
    cfor(0)(_ < len, _ + 1) { i =>
      arr(i) = ps(i).jtsGeom
    }

    MultiPolygon(factory.createMultiPolygon(arr))
  }

  implicit def jts2MultiPolygon(jtsGeom: jts.MultiPolygon): MultiPolygon = apply(jtsGeom)
}

case class MultiPolygon(jtsGeom: jts.MultiPolygon) extends MultiGeometry
                                                   with Relatable
                                                   with TwoDimensions {

  /** Returns a unique representation of the geometry based on standard coordinate ordering. */
  def normalized(): MultiPolygon = { 
    val geom = jtsGeom.clone.asInstanceOf[jts.MultiPolygon]
    geom.normalize
    MultiPolygon(geom)
  }

  /** Returns the Polygons contained in MultiPolygon. */
  lazy val polygons: Array[Polygon] = {
    for (i <- 0 until jtsGeom.getNumGeometries) yield {
      Polygon(jtsGeom.getGeometryN(i).clone.asInstanceOf[jts.Polygon])
    }
  }.toArray

  lazy val area: Double =
    jtsGeom.getArea

  lazy val boundary: MultiLineResult =
    jtsGeom.getBoundary

  /** Returns this MulitPolygon's vertices. */
  lazy val vertices: Array[Point] = {
    val coords = jtsGeom.getCoordinates
    val arr = Array.ofDim[Point](coords.size)
    cfor(0)(_ < arr.size, _ + 1) { i =>
      val coord = coords(i)
      arr(i) = Point(coord.x, coord.y)
    }
    arr
  }

  /** Get the number of vertices in this geometry */
  lazy val vertexCount: Int = jtsGeom.getNumPoints

  // -- Intersection

  def intersection(): MultiPolygonMultiPolygonIntersectionResult =
    polygons.map(_.jtsGeom).reduce[jts.Geometry] {
      _.intersection(_)
    }

  def &(p: Point): PointOrNoResult =
    intersection(p)
  def intersection(p: Point): PointOrNoResult =
    p.intersection(this)
  def safeIntersection(p: Point): PointOrNoResult =
    try intersection(p)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(p.jtsGeom))
    }

  def &(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(l)
  def intersection(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    l.intersection(this)
  def safeIntersection(l: Line): OneDimensionAtLeastOneDimensionIntersectionResult =
    try intersection(l)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(l.jtsGeom))
    }

  def &(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    intersection(g)
  def intersection(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)
  def safeIntersection(g: TwoDimensions): TwoDimensionsTwoDimensionsIntersectionResult =
    try intersection(g)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(g.jtsGeom))
    }

  def &(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    ls.intersection(this)
  def safeIntersection(ls: MultiLine): OneDimensionAtLeastOneDimensionIntersectionResult =
    try intersection(ls)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(ls.jtsGeom))
    }

  // -- Union

  def |(p: Point): PointMultiPolygonUnionResult =
    union(p)

  def union(p: Point): PointMultiPolygonUnionResult =
    jtsGeom.union(p.jtsGeom)

  def |(l: Line): LineMultiPolygonUnionResult =
    union(l)
  def union(l: Line): LineMultiPolygonUnionResult =
    l.union(this)

  def |(p: Polygon): TwoDimensionsTwoDimensionsUnionResult =
    union(p)

  def union(p: Polygon): TwoDimensionsTwoDimensionsUnionResult = {
    (this.polygons :+ p).toSeq.unionGeometries
  }

  def |(ps: MultiPoint): LineMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPoint): LineMultiPolygonUnionResult =
    jtsGeom.union(ps.jtsGeom)

  def |(ls: MultiLine) = union(ls)
  def union(ls: MultiLine): LineMultiPolygonUnionResult =
    jtsGeom.union(ls.jtsGeom)

  def |(ps: MultiPolygon): TwoDimensionsTwoDimensionsUnionResult =
    union(ps)
  def union(ps: MultiPolygon): TwoDimensionsTwoDimensionsUnionResult =
    (this.polygons ++ ps.polygons).toSeq.unionGeometries

  def union: TwoDimensionsTwoDimensionsUnionResult =
    polygons.toSeq.unionGeometries

  // -- Difference

  def difference(): MultiPolygonMultiPolygonDifferenceResult =
    polygons.map(_.jtsGeom).reduce[jts.Geometry] {
      _.difference(_)
    }

  def -(p: Point): MultiPolygonXDifferenceResult =
    difference(p)
  def difference(p: Point): MultiPolygonXDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(l: Line): MultiPolygonXDifferenceResult =
    difference(l)
  def difference(l: Line): MultiPolygonXDifferenceResult =
    jtsGeom.difference(l.jtsGeom)

  def -(p: Polygon): TwoDimensionsTwoDimensionsDifferenceResult =
    difference(p)
  def difference(p: Polygon): TwoDimensionsTwoDimensionsDifferenceResult =
    jtsGeom.difference(p.jtsGeom)

  def -(ps: MultiPoint): MultiPolygonXDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): MultiPolygonXDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  def -(ls: MultiLine): MultiPolygonXDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): MultiPolygonXDifferenceResult =
    jtsGeom.difference(ls.jtsGeom)

  def -(ps: MultiPolygon): TwoDimensionsTwoDimensionsDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): TwoDimensionsTwoDimensionsDifferenceResult =
    jtsGeom.difference(ps.jtsGeom)

  // -- SymDifference

  def symDifference(): MultiPolygonMultiPolygonSymDifferenceResult =
    polygons.map(_.jtsGeom).reduce[jts.Geometry] {
      _.symDifference(_)
    }

  def symDifference(g: ZeroDimensions): PointMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: OneDimension): LineMultiPolygonSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  def symDifference(g: TwoDimensions): TwoDimensionsTwoDimensionsSymDifferenceResult =
    jtsGeom.symDifference(g.jtsGeom)

  // -- Predicates

  def contains(g: Geometry): Boolean =
    jtsGeom.contains(g.jtsGeom)

  def coveredBy(g: TwoDimensions): Boolean =
    jtsGeom.coveredBy(g.jtsGeom)

  def covers(g: Geometry): Boolean =
    jtsGeom.covers(g.jtsGeom)

  def crosses(g: OneDimension): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  def crosses(ps: MultiPoint): Boolean =
    jtsGeom.crosses(ps.jtsGeom)

  def overlaps(g: TwoDimensions): Boolean =
    jtsGeom.crosses(g.jtsGeom)

  def touches(g: AtLeastOneDimension): Boolean =
    jtsGeom.touches(g.jtsGeom)

  def within(g: TwoDimensions): Boolean =
    jtsGeom.within(g.jtsGeom)
}
