package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

case class PolygonSet(ps:Set[Polygon]) extends GeometrySet {
  val geom = factory.createMultiPolygon(ps.map(_.geom).toArray)

  def &(l:Line) = intersection(l)
  def intersection(l:Line):LineSetIntersectionResult = 
    geom.intersection(l.geom)

  def &(p:Polygon) = intersection(p)
  def intersection(p:Polygon):PolygonSetIntersectionResult = 
    geom.intersection(p.geom)

  def &(ls:LineSet):LineSetIntersectionResult = intersection(ls)
  def intersection(ls:LineSet):LineSetIntersectionResult = ls.intersection(this)

  def &(ps:PolygonSet) = intersection(ps)
  def intersection(ps:PolygonSet):PolygonSetIntersectionResult =
    geom.intersection(ps.geom)

  def |(p:Point) = union(p)
  def union(p:Point):PolygonSetUnionResult =
    geom.union(p.geom)

  def |(l:Line) = union(l)
  def union(l:Line):PolygonSetUnionResult =
    geom.union(l.geom)

  def |(p:Polygon) = union(p)
  def union(p:Polygon):PolygonPolygonUnionResult =
    geom.union(p.geom)

  lazy val area:Double = geom.getArea
}
