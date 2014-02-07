package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

case class LineSet(ls:Set[Line]) extends GeometrySet {
  val geom = factory.createMultiLineString(ls.map(_.geom).toArray)

  def &(l:Line) = intersection(l)
  def intersection(l:Line):LineSetIntersectionResult = 
    geom.intersection(l.geom)

  def &(p:Polygon) = intersection(p)
  def intersection(p:Polygon):LineSetIntersectionResult = 
    geom.intersection(p.geom)

  def &(ls:LineSet) = intersection(ls)
  def intersection(ls:LineSet):LineSetIntersectionResult = 
    geom.intersection(ls.geom)

  def &(ps:PolygonSet) = intersection(ps)
  def intersection(ps:PolygonSet):LineSetIntersectionResult = 
    geom.intersection(ps.geom)
}
