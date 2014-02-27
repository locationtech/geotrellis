package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Line {

  implicit def jtsToLine(geom: jts.LineString): Line =
    apply(geom)

  def apply(geom: jts.LineString): Line =
    Line(geom, geom.getCoordinates.map(c => Point(c.x, c.y)).toList)

  def apply(points: Point*): Line =
    apply(points.toList)

  def apply(points: Seq[Point])(implicit d: DummyImplicit): Line =
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

case class Line(geom: jts.LineString, points: List[Point]) extends Geometry 
                                                              with TwoDimensions {

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

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): LineLineIntersectionResult =
    intersection(l)
  def intersection(l: Line): LineLineIntersectionResult =
    geom.intersection(l.geom)

  def &(p: Polygon): LinePolygonIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): LinePolygonIntersectionResult =
    geom.intersection(p.geom)

  def &(ps: PointSet): PointSetIntersectionResult =
    intersection(ps)
  def intersection(ps:PointSet):PointSetIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet): LineSetIntersectionResult =
    intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet): LineSetIntersectionResult =
    intersection(ps)
  def intersection(ps: PolygonSet): LineSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): PointLineUnionResult =
    union(p)
  def union(p: Point): PointLineUnionResult =
    p.union(this)

  def |(l: Line): LineLineUnionResult =
    union(l)
  def union(l: Line): LineLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    geom.union(p.geom)

  def |(ps: PointSet): PointLineUnionResult =
    union(ps)
  def union(ps: PointSet): PointLineUnionResult =
    geom.union(ps.geom)

  def |(ls: LineSet): LineLineUnionResult =
    union(ls)
  def union(ls: LineSet): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    union(ps)
  def union(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): LinePointDifferenceResult =
    difference(p)
  def difference(p: Point): LinePointDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): LineXDifferenceResult =
    difference(l)
  def difference(l: Line): LineXDifferenceResult =
    geom.difference(l.geom)

  def -(p: Polygon): LineXDifferenceResult =
    difference(p)
  def difference(p: Polygon): LineXDifferenceResult =
    geom.difference(p.geom)

  def -(ps: PointSet): LinePointDifferenceResult =
    difference(ps)
  def difference(ps: PointSet): LinePointDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: LineSet): LineXDifferenceResult =
    difference(ls)
  def difference(ls: LineSet): LineXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: PolygonSet): LineXDifferenceResult =
    difference(ps)
  def difference(ps: PolygonSet): LineXDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(p: Polygon): OneDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)
  
  def symDifference(ps: PolygonSet): OneDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(ps.geom)

  // -- Buffer

  def buffer(d:Double):Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def contains(g: AtMostOneDimensions): Boolean =
    geom.contains(g.geom)

  def within(g: AtLeastOneDimensions): Boolean =
    geom.within(g.geom)

  def crosses(g: AtLeastOneDimensions): Boolean =
    geom.crosses(g.geom)

  def crosses(ps: PointSet): Boolean =
    geom.crosses(ps.geom)

  def overlaps(g: OneDimensions): Boolean =
    geom.overlaps(g.geom)

}
