package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

case class LineSet(ls: Set[Line]) extends GeometrySet {

  val geom = factory.createMultiLineString(ls.map(_.geom).toArray)

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): LineSetIntersectionResult =
    l.intersection(this)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): LineSetIntersectionResult =
    p.intersection(this)

  def &(ps: PointSet) = intersection(ps)
  def intersection(ps: PointSet): PointSetIntersectionResult =
    ps.intersection(this)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): LineSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Predicates

  def crosses(g: Geometry): Boolean =
    geom.crosses(g.geom)
}
