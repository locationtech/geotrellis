package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._

object LineSet {
  def apply(ls: Line*): LineSet = 
    LineSet(ls.toSet)
  def apply(ls: Seq[Line])(implicit d: DummyImplicit): LineSet = 
    LineSet(ls.toSet)
}

case class LineSet(ls: Set[Line]) extends GeometrySet 
                                     with TwoDimensions {

  val geom = factory.createMultiLineString(ls.map(_.geom).toArray)

  lazy val isClosed: Boolean =
    geom.isClosed

  lazy val boundary: LineBoundaryResult =
    geom.getBoundary

  // -- Intersection

  def &(p: Point) = intersection(p)
  def intersection(p: Point): PointIntersectionResult =
    p.intersection(this)

  def &(l: Line) = intersection(l)
  def intersection(l: Line): LineSetIntersectionResult =
    l.intersection(this)

  def &(p: Polygon) = intersection(p)
  def intersection(p: Polygon): LineSetIntersectionResult =
    p.intersection(this)

  def &(ps: PointSet) = intersection(ps)
  def intersection(ps: PointSet): PointSetIntersectionResult =
    ps.intersection(this)

  def &(ls: LineSet) = intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet) = intersection(ps)
  def intersection(ps: PolygonSet): LineSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PointLineSetUnionResult =
    p.union(this)

  def |(l: Line) = union(l)
  def union(l:Line): LineLineUnionResult =
    l.union(this)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonXUnionResult =
    p.union(this)

  def |(ps: PointSet) = union(ps)
  def union(ps: PointSet): PointLineSetUnionResult =
    ps.union(this)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point) = difference(p)
  def difference(p: Point): LineSetPointDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line) = difference(l)
  def difference(l: Line): LineXDifferenceResult = {
    geom.difference(l.geom)
  }

  def -(p: Polygon) = difference(p)
  def difference(p: Polygon): LineXDifferenceResult = {
    geom.difference(p.geom)
  }

  def -(ps: PointSet) = difference(ps)
  def difference(ps: PointSet): LineSetPointDifferenceResult = {
    geom.difference(ps.geom)
  }

  def -(ls: LineSet) = difference(ls)
  def difference(ls: LineSet): LineXDifferenceResult = {
    geom.difference(ls.geom)
  }

  def -(ps: PolygonSet) = difference(ps)
  def difference(ps: PolygonSet): LineXDifferenceResult = {
    geom.difference(ps.geom)
  }

  // -- Predicates

}
