package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Point {

  def apply(x: Double, y: Double): Point =
    Point(factory.createPoint(new jts.Coordinate(x,y)))

  implicit def jts2Point(geom: jts.Point): Point = apply(geom)

}

case class Point(geom: jts.Point) extends Geometry {

  assert(!geom.isEmpty)

  val x = geom.getX
  val y = geom.getY

  // -- Intersection

  def &(other: Geometry) = intersection(other)
  def intersection(other: Geometry): PointIntersectionResult =
    geom.intersection(other.geom)

  // -- Union

  def |(p: Point) = union(p)
  def union(p: Point): PointPointUnionResult =
    geom.union(p.geom)

  def |(l: Line) = union(l)
  def union(l: Line): PointLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon) = union(p)
  def union(p: Polygon): PolygonXUnionResult =
    geom.union(p.geom)

  def |(ps: PointSet) = union(ps)
  def union(ps: PointSet): PointPointUnionResult =
    geom.union(ps.geom)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): PointLineSetUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet) = union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry) = difference(other)
  def difference(other: Geometry): PointDifferenceResult =
    geom.difference(other.geom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def within(l: Line): Boolean =
    geom.within(l.geom)

  def within(p: Polygon): Boolean =
    geom.within(p.geom)

  def crosses(g: Geometry): Boolean =
    geom.crosses(g.geom)
}
