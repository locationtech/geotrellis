package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class PointSet(ps: Set[Point]) extends GeometrySet 
                                       with ZeroDimensions {

  val geom = factory.createMultiPoint(ps.map(_.geom).toArray)

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): PointSetIntersectionResult =
    l.intersection(this)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): PointSetIntersectionResult =
    p.intersection(this)

  def &(ps: PointSet) = intersection(ps)
  def intersection(ps: PointSet): PointSetIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): PointSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): PointSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PointPointUnionResult =
    p.union(this)

  def |(l: Line) = union(l)
  def union(l:Line): PointLineUnionResult =
    l.union(this)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonXUnionResult =
    p.union(this)

  def |(ps: PointSet) = union(ps)
  def union(ps: PointSet): PointPointUnionResult =
    geom.union(ps.geom)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): PointLineSetUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry) = difference(other)
  def difference(other: Geometry): PointSetDifferenceResult =
    geom.difference(other.geom)

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    geom.contains(g.geom)

  def within(ps: PointSet): Boolean =
    geom.within(ps.geom)

  def within(g: AtLeastOneDimensions): Boolean =
    geom.within(g.geom)

  // -- Misc.

  def convexHull: Polygon =
    geom.convexHull.asInstanceOf[jts.Polygon]

}
