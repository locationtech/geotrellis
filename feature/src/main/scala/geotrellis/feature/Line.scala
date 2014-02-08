package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

case class Line(geom: jts.LineString, points: List[Point]) extends Geometry {

  assert(!geom.isEmpty)

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): LineLineIntersectionResult =
    geom.intersection(l.geom)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): PolygonLineIntersectionResult =
    geom.intersection(p.geom)

  def &(ps: PointSet) = intersection(ps)
  def intersection(ps:PointSet):PointSetIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): LineSetIntersectionResult =  // should we define PolygonSetIntersectionResult??
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): LinePointUnionResult =
    p.union(this)

  def |(l: Line) = union(l)
  def union(l: Line): LineLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonXUnionResult =
    geom.union(p.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def difference(p: Point): LinePointDifferenceResult =
    geom.difference(p.geom)

  def difference(l: Line): LineXDifferenceResult = {
    geom.difference(l.geom)
  }

  def difference(p: Polygon): LineXDifferenceResult = {
    geom.difference(p.geom)
  }

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

  // -- Buffer

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]


  // Not sure what to do about LinearString, if it really
  // needs to be around...will make construction of Polys 
  // tougher maybe.

  // -- Predicates

  def isClosed: Boolean =
    geom.isClosed

  def contains(p: Point): Boolean =
    geom.contains(p.geom)

  def contains(l: Line): Boolean =
    geom.contains(l.geom)

  def within(l: Line): Boolean =
    geom.within(l.geom)

  def within(p: Polygon): Boolean =
    geom.within(p.geom)

  def crosses(g: Geometry): Boolean =
    geom.crosses(g.geom)

}

object Line {

  implicit def jtsToLine(geom: jts.LineString): Line =
    apply(geom)

  def apply(geom: jts.LineString): Line =
    Line(geom, geom.getCoordinates.map(c => Point(c.x, c.y)).toList)

  def apply(points: Seq[Point]): Line =
    apply(points.toList)

  def apply(points: Array[Point]): Line =
    apply(points.toList)

  def apply(points: List[Point]): Line = {
    if (points.length < 2) {
      sys.error("Invalid line: Requires 2 or more points.")
    }

    Line(factory.createLineString(points.map(_.geom.getCoordinate).toArray), points)
  }

}