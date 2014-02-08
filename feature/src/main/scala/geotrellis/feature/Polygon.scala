package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class Polygon(geom: jts.Polygon) extends Geometry {

  assert(!geom.isEmpty)

  lazy val exterior = Line(geom.getExteriorRing)

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): PolygonLineIntersectionResult =
    l.intersection(this)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): PolygonPolygonIntersectionResult =
    geom.intersection(p.geom)

  def &(ps: PointSet) = intersection(ps)
  def intersection(ps: PointSet): PointSetIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): PolygonSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PolygonXUnionResult =
    p.union(this)

  def |(l:Line) = union(l)
  def union(l:Line):PolygonXUnionResult =
    l.union(this)

  def |(p:Polygon) = union(p)
  def union(p:Polygon):PolygonPolygonUnionResult =
    geom.union(p.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonPolygonUnionResult =
    geom.union(ps.geom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def contains(p: Point): Boolean =
    geom.contains(p.geom)

  def contains(l: Line): Boolean =
    geom.contains(l.geom)

  def contains(p: Polygon): Boolean =
    geom.contains(p.geom)

  def within(p: Polygon): Boolean =
    geom.within(p.geom)

  def crosses(g: Geometry): Boolean =
    geom.crosses(g.geom)

  lazy val isRectangle = geom.isRectangle

  lazy val area = geom.getArea
}

object Polygon {

  implicit def jtsToPolygon(geom: jts.Polygon): Polygon =
    Polygon(geom)

  def apply(exterior: Line): Polygon =
    apply(exterior, Set())

  def apply(exterior: Line, holes:Set[Line]): Polygon = {
    if(!exterior.isClosed) {
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior")
    }

    if(exterior.points.length < 4) {
      sys.error(s"Cannot create a polygon with exterior with less that 4 points: $exterior")
    }

    val extGeom = factory.createLinearRing(exterior.geom.getCoordinates)

    val holeGeoms = (
      for (hole <- holes) yield {
        if (!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if (hole.points.length < 4)
            sys.error(s"Cannot create a polygon with a hole with less that 4 points: $hole")
          else
            factory.createLinearRing(hole.geom.getCoordinates)
        }
      }).toArray

    factory.createPolygon(extGeom, holeGeoms)
  }
}