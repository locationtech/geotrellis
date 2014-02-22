package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

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

case class Line(geom: jts.LineString, points: List[Point]) extends Geometry {

  assert(!geom.isEmpty)

  lazy val isClosed: Boolean =
    geom.isClosed

  lazy val isSimple: Boolean =
    geom.isSimple

  lazy val boundary: LineBoundaryResult =
    geom.getBoundary

  lazy val vertices: PointSet =
    geom.getCoordinates

  lazy val boundingBox: Option[Polygon] =
    if (geom.isEmpty) None else Some(geom.getEnvelope.asInstanceOf[Polygon])

  lazy val length: Double =
    geom.getLength

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
  def intersection(ps: PolygonSet): LineSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PointLineUnionResult =
    p.union(this)

  def |(l: Line) = union(l)
  def union(l: Line): LineLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonXUnionResult =
    geom.union(p.geom)

  def |(ps: PointSet) = union(ps)
  def union(ps: PointSet): PointLineUnionResult =
    geom.union(ps.geom)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point) = difference(p)
  def difference(p: Point): LinePointDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line) = difference(l)
  def difference(l: Line): LineXDifferenceResult =
    geom.difference(l.geom)

  def -(p: Polygon) = difference(p)
  def difference(p: Polygon): LineXDifferenceResult =
    geom.difference(p.geom)

  def -(ps: PointSet) = difference(ps)
  def difference(ps: PointSet): LinePointDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: LineSet) = difference(ls)
  def difference(ls: LineSet): LineXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: PolygonSet) = difference(ps)
  def difference(ps: PolygonSet): LineXDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(p: Point): PointLineSymDifferenceResult =
    p.symDifference(this)

  def symDifference(l: Line): LineLineSymDifferenceResult =
    geom.symDifference(l.geom)

  def symDifference(p: Polygon): LinePolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  // -- Buffer

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def contains(p: Point): Boolean =
    geom.contains(p.geom)

  def contains(l: Line): Boolean =
    geom.contains(l.geom)

  def within(l: Line): Boolean =
    geom.within(l.geom)

  def within(p: Polygon): Boolean =
    geom.within(p.geom)

  def crosses(p: Point): Boolean =
    geom.crosses(p.geom)

  def crosses(l: Line): Boolean =
    geom.crosses(l.geom)

  def crosses(p: Polygon): Boolean =
    geom.crosses(p.geom)

  def overlaps(l: Line): Boolean =
    geom.overlaps(l.geom)

}
