/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class PolygonSet(ps: Set[Polygon]) extends GeometrySet 
                                           with TwoDimensions {

  val geom = factory.createMultiPolygon(ps.map(_.geom).toArray)

  lazy val area: Double = geom.getArea

  lazy val boundary: LineSetResult =
    geom.getBoundary

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): LineSetIntersectionResult =
    l.intersection(this)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): PolygonSetIntersectionResult =
    p.intersection(this)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    ls.intersection(this)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): PolygonSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PolygonSetUnionResult =
    p.union(this)

  def |(l: Line) = union(l)
  def union(l: Line): PolygonSetUnionResult =
    l.union(this)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    p.union(this)

  def |(ps: PointSet) = union(ps)
  def union(ps: PointSet): PolygonSetUnionResult =
    ps.union(this)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): PolygonSetUnionResult =
    ls.union(this)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point) = difference(p)
  def difference(p: Point): PolygonSetXDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line) = difference(l)
  def difference(l: Line): PolygonSetXDifferenceResult = {
    geom.difference(l.geom)
  }

  def -(p: Polygon) = difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult = {
    geom.difference(p.geom)
  }

  def -(ps: PointSet) = difference(ps)
  def difference(ps: PointSet): PolygonSetXDifferenceResult = {
    geom.difference(ps.geom)
  }

  def -(ls: LineSet) = difference(ls)
  def difference(ls: LineSet): PolygonSetXDifferenceResult = {
    geom.difference(ls.geom)
  }

  def -(ps: PolygonSet) = difference(ps)
  def difference(ps: PolygonSet): PolygonPolygonDifferenceResult = {
    geom.difference(ps.geom)
  }

  // -- Predicates

  def contains(g: Geometry): Boolean =
    geom.contains(g.geom)

  def within(g: TwoDimensions): Boolean =
    geom.within(g.geom)

  def crosses(g: OneDimensions): Boolean =
    geom.crosses(g.geom)
}
