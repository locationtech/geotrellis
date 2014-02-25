package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Point {

  def apply(x: Double, y: Double): Point =
    Point(factory.createPoint(new jts.Coordinate(x, y)))

  implicit def jts2Point(geom: jts.Point): Point = apply(geom)

}

case class Point(geom: jts.Point) extends Geometry 
                                     with ZeroDimensions {

  assert(!geom.isEmpty)

  val x: Double =
    geom.getX
  val y: Double =
    geom.getY

  // -- Intersection

  def &(other: Geometry): PointIntersectionResult =
    intersection(other)
  def intersection(other: Geometry): PointIntersectionResult =
    geom.intersection(other.geom)

  // -- Union

  def |(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    union(g)
  def union(g: ZeroDimensions): PointZeroDimensionsUnionResult =
    geom.union(g.geom)

  def |(l: Line): PointLineUnionResult =
    union(l)
  def union(l: Line): PointLineUnionResult =
    geom.union(l.geom)

  def |(p: Polygon): PolygonXUnionResult =
    union(p)
  def union(p: Polygon): PolygonXUnionResult =
    geom.union(p.geom)

  def |(ls: LineSet): PointLineSetUnionResult =
    union(ls)
  def union(ls: LineSet): PointLineSetUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet): PolygonSetUnionResult =
    union(ps)
  def union(ps: PolygonSet): PolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry): PointDifferenceResult =
    difference(other)
  def difference(other: Geometry): PointDifferenceResult =
    geom.difference(other.geom)

  // -- SymDifference

  def symDifference(p: Point): PointPointSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(l.geom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ps: PointSet): ZeroDimensionsPointSetSymDifferenceResult =
    geom.symDifference(ps.geom)

  def symDifference(ls: LineSet): ZeroDimensionsLineSetSymDifferenceResult =
    geom.symDifference(ls.geom)

  def symDifference(ps: PolygonSet): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(ps.geom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    geom.contains(g.geom)

  def within(g: Geometry): Boolean =
    geom.within(g.geom)

}
