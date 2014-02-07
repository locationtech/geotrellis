package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

case class PointSet(ps:Set[Point]) extends GeometrySet {
  val geom = factory.createMultiPoint(ps.map(_.geom).toArray)

  def &(l:Line) = intersection(l)
  def intersection(l:Line):PointSetIntersectionResult =
    geom.intersection(l.geom)

  def &(p:Polygon) = intersection(p)
  def intersection(p:Polygon):PointSetIntersectionResult =
    geom.intersection(p.geom)

  def &(ls:LineSet) = intersection(ls)
  def intersection(ls:LineSet):PointSetIntersectionResult = ls.intersection(this)

  def &(ps:PolygonSet) = intersection(ps)
  def intersection(ps:PolygonSet):PointSetIntersectionResult = ps.intersection(this)

  def convexHull:Polygon = 
    geom.convexHull.asInstanceOf[jts.Polygon]

}
