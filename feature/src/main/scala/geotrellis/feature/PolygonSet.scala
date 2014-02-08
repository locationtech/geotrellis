package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class PolygonSet(ps: Set[Polygon]) extends GeometrySet {

  val geom = factory.createMultiPolygon(ps.map(_.geom).toArray)

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

  // -- Predicates
  def crosses(g: Geometry): Boolean =
    geom.crosses(g.geom)

  lazy val area: Double = geom.getArea
}
