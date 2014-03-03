package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._
import com.vividsolutions.jts.geom.MultiPoint

case class PointSet(ps: Set[Point]) extends GeometrySet 
                                       with ZeroDimensions {

  val geom: MultiPoint =
    factory.createMultiPoint(ps.map(_.geom).toArray)

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): PointSetGeometryIntersectionResult =
    intersection(l)
  def intersection(l: Line): PointSetGeometryIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): PointSetGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): PointSetGeometryIntersectionResult =
    p.intersection(this)

  def &(ps: PointSet): PointSetGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: PointSet): PointSetGeometryIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet): PointSetGeometryIntersectionResult =
    intersection(ls)
  def intersection(ls: LineSet): PointSetGeometryIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet): PointSetGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: PolygonSet): PointSetGeometryIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): PointZeroDimensionsUnionResult =
    union(p)
  def union(p: Point): PointZeroDimensionsUnionResult =
    p.union(this)

  def |(l: Line): PointLineUnionResult =
    union(l)
  def union(l:Line): PointLineUnionResult =
    l.union(this)

  def |(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    p.union(this)

  def |(ps: PointSet): PointZeroDimensionsUnionResult =
    union(ps)
  def union(ps: PointSet): PointZeroDimensionsUnionResult =
    geom.union(ps.geom)

  def |(ls: LineSet): PointLineSetUnionResult =
    union(ls)
  def union(ls: LineSet): PointLineSetUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    union(ps)
  def union(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry): PointSetDifferenceResult =
    difference(other)
  def difference(other: Geometry): PointSetDifferenceResult =
    geom.difference(other.geom)

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    geom.contains(g.geom)

  def within(g: Geometry): Boolean =
    geom.within(g.geom)

  def overlaps(ps: PointSet): Boolean =
    geom.overlaps(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsPointSetSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(l.geom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ls: LineSet): ZeroDimensionsLineSetSymDifferenceResult =
    geom.symDifference(ls.geom)

  def symDifference(ps: PolygonSet): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(ps.geom)
                         
  // -- Misc.

  def convexHull: Polygon =
    geom.convexHull.asInstanceOf[jts.Polygon]

}
