package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

object Line {
  implicit def jtsToLine(geom:jts.LineString):Line = apply(geom)

  def apply(geom:jts.LineString):Line =
    Line(geom,geom.getCoordinates.map(c => Point(c.x,c.y)).toList)

  def apply(points:Seq[Point]):Line =
    apply(points.toList)
  def apply(points:Array[Point]):Line =
    apply(points.toList)
  def apply(points:List[Point]):Line = {
    if(points.length < 2) { sys.error("Invalid line: Requires 2 or more points.") }
    Line(factory.createLineString(points.map(_.geom.getCoordinate).toArray),points)
  }
}

case class Line(geom:jts.LineString,points:List[Point]) extends Geometry {
  assert(!geom.isEmpty)

  def &(p:Point) = intersection(p)
  def intersection(p:Point):PointIntersectionResult = p.intersection(this)

  def &(l:Line) = intersection(l)
  def intersection(l:Line):LineLineIntersectionResult =
    geom.intersection(l.geom)

  def &(p:Polygon) = intersection(p)
  def intersection(p:Polygon):PolygonLineIntersectionResult = 
    geom.intersection(p.geom)

  def &(ps:PointSet) = intersection(ps)
  def intersection(ps:PointSet):PointSetIntersectionResult = ps.intersection(this)

  def &(ls:LineSet) = intersection(ls)
  def intersection(ls:LineSet):LineSetIntersectionResult = ls.intersection(this)

  def &(ps:PolygonSet) = intersection(ps)
  def intersection(ps:PolygonSet):LineSetIntersectionResult = ps.intersection(this)

  def |(p:Point) = union(p)
  def union(p:Point):LinePointUnionResult =
    geom.union(p.geom)

  def |(l:Line) = union(l)
  def union(l:Line):LineLineUnionResult =
    geom.union(l.geom)

  def |(p:Polygon) = union(p)
  def union(p:Polygon):PolygonXUnionResult = p.union(this)

  // Not sure what to do about LinearString, if it really
  // needs to be around...will make construction of Polys 
  // tougher maybe.
  def isClosed = geom.isClosed

  def crosses(g:Geometry) = geom.crosses(g.geom)

  def contains(p:Point) = geom.contains(p.geom)
  def contains(l:Line) =  geom.contains(l.geom)
  def within(l:Line) = geom.within(l.geom)
  def within(p:Polygon) = geom.within(p.geom)

  def difference(g:Geometry):Set[Line] =
    geom.difference(g.geom) match {
      case ml:jts.MultiLineString =>
        ml
      case l:jts.LineString =>
        Set(Line(l))
      case x =>
        assert(x.isEmpty)
        Set()
    }

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]
}
