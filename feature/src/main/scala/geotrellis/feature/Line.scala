package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Line {

  implicit def jtsToLine(geom: jts.LineString): Line =
    apply(geom)

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

    Line(factory.createLineString(points.map(_.geom.getCoordinate).toArray))
  }

}

case class Line(geom: jts.LineString) extends Geometry 
                                         with TwoDimensions {

  assert(!geom.isEmpty)

  lazy val points: List[Point] = geom.getCoordinates.map(c => Point(c.x, c.y)).toList

  lazy val isClosed: Boolean =
    geom.isClosed

  lazy val isSimple: Boolean =
    geom.isSimple

  lazy val boundary: LineBoundaryResult =
    geom.getBoundary

  lazy val vertices: MultiPoint =
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

  def &(ps: MultiPoint): MultiPointIntersectionResult =
    intersection(ps)
  def intersection(ps:MultiPoint):MultiPointIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: MultiLine): MultiLineIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiLineIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: MultiPolygon): MultiLineIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiLineIntersectionResult =
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

  def |(ps: MultiPoint): PointLineUnionResult =
    union(ps)
  def union(ps: MultiPoint): PointLineUnionResult =
    geom.union(ps.geom)

  def |(ls: MultiLine): LineLineUnionResult =
    union(ls)
  def union(ls: MultiLine): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
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

  def -(ps: MultiPoint): LinePointDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): LinePointDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: MultiLine): LineXDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): LineXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: MultiPolygon): LineXDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): LineXDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(p: Polygon): OneDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)
  
  def symDifference(ps: MultiPolygon): OneDimensionsMultiPolygonSymDifferenceResult =
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

  def crosses(ps: MultiPoint): Boolean =
    geom.crosses(ps.geom)

  def overlaps(g: OneDimensions): Boolean =
    geom.overlaps(g.geom)

}
