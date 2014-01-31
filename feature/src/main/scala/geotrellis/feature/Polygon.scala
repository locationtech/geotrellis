package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

object Polygon {
  implicit def jtsToPolygon(geom:jts.Polygon):Polygon = Polygon(geom)

  def apply(exterior:Line):Polygon = 
    apply(exterior,Set())

  def apply(exterior:Line,holes:Set[Line]):Polygon = {
    if(!exterior.isClosed) { 
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior") 
    }
    if(exterior.points.length < 4) { 
      sys.error(s"Cannot create a polygon with exterior with less that 4 points: $exterior") 
    }
    val extGeom = factory.createLinearRing(exterior.geom.getCoordinates)
    val holeGeoms = 
      (for(hole <- holes) yield {
        if(!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if(hole.points.length < 4) 
            sys.error(s"Cannot create a polygon with a hole with less that 4 points: $hole")
          else
            factory.createLinearRing(hole.geom.getCoordinates)
        }
      }).toArray
    factory.createPolygon(extGeom,holeGeoms)
  }
}

case class Polygon(geom:jts.Polygon) extends Geometry {
  assert(!geom.isEmpty)

  lazy val exterior = Line(geom.getExteriorRing)

  def &(p:Point) = intersection(p)
  def intersection(p:Point):PointIntersectionResult = p.intersection(this)

  def &(l:Line) = intersection(l)
  def intersection(l:Line):PolygonLineIntersectionResult = l.intersection(this)

  def &(p:Polygon) = intersection(p)
  def intersection(p:Polygon):PolygonPolygonIntersectionResult =
    geom.intersection(p.geom)

  def &(ps:PointSet) = intersection(ps)
  def intersection(ps:PointSet):PointSetIntersectionResult = ps.intersection(this)

  def &(ls:LineSet) = intersection(ls)
  def intersection(ls:LineSet):LineSetIntersectionResult = ls.intersection(this)

  def &(ps:PolygonSet) = intersection(ps)
  def intersection(ps:PolygonSet):PolygonSetIntersectionResult = ps.intersection(this)

  def |(p:Point) = union(p)
  def union(p:Point):PolygonXUnionResult =
    geom.union(p.geom)

  def |(l:Line) = union(l)
  def union(l:Line):PolygonXUnionResult =
    geom.union(l.geom)

  def |(p:Polygon) = union(p)
  def union(p:Polygon):PolygonPolygonUnionResult =
    geom.union(p.geom) 

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  def contains(p:Point) = geom.contains(p.geom)
  def contains(l:Line) = geom.contains(l.geom)
  def contains(p:Polygon) = geom.contains(p.geom)

  def within(p:Polygon) = geom.within(p.geom)

  lazy val isRectangle = geom.isRectangle

  lazy val area = geom.getArea
}
