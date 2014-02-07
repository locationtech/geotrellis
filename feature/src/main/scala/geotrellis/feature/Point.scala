package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

case class Point(geom:jts.Point) extends Geometry {
  assert(!geom.isEmpty)
  val x = geom.getX
  val y = geom.getY

  def &(other:Geometry) = intersection(other)
  def intersection(other:Geometry):PointIntersectionResult =
    geom.intersection(other.geom)

  def |(p:Point) = union(p)
  def union(p:Point):PointPointUnionResult =
    geom.union(p.geom)

  def |(l:Line) = union(l)
  def union(l:Line):LinePointUnionResult = l.union(this)

  def |(p:Polygon) = union(p)
  def union(p:Polygon):PolygonXUnionResult = p.union(this)

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  def within(l:Line) = geom.within(l.geom)
  def within(p:Polygon) = geom.within(p.geom)
}

object Point {
  def apply(x:Double,y:Double):Point =
    Point(factory.createPoint(new jts.Coordinate(x,y)))

  implicit def jts2Point(geom:jts.Point):Point = apply(geom)
}
