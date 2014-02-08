package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class PointSet(ps: Set[Point]) extends GeometrySet {

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

  def convexHull: Polygon =
    geom.convexHull.asInstanceOf[jts.Polygon]

}
