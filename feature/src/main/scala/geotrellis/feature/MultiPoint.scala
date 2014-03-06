package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object MultiPoint {
  def apply(ps: Point*): MultiPoint = 
    apply(ps)

  def apply(ps: Traversable[Point]): MultiPoint =
    MultiPoint(factory.createMultiPoint(ps.map(_.geom).toArray))
}

case class MultiPoint(geom: jts.MultiPoint) extends MultiGeometry 
                                             with ZeroDimensions {

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): MultiPointIntersectionResult =
    intersection(l)
  def intersection(l: Line): MultiPointIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): MultiPointIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): MultiPointIntersectionResult =
    p.intersection(this)

  def &(ps: MultiPoint): MultiPointIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPoint): MultiPointIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: MultiLine): MultiPointIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiPointIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: MultiPolygon): MultiPointIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiPointIntersectionResult =
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

  def |(ps: MultiPoint): PointZeroDimensionsUnionResult =
    union(ps)
  def union(ps: MultiPoint): PointZeroDimensionsUnionResult =
    geom.union(ps.geom)

  def |(ls: MultiLine): PointMultiLineUnionResult =
    union(ls)
  def union(ls: MultiLine): PointMultiLineUnionResult =
    geom.union(ls.geom)

  def |(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(other: Geometry): MultiPointDifferenceResult =
    difference(other)
  def difference(other: Geometry): MultiPointDifferenceResult =
    geom.difference(other.geom)

  // -- Predicates

  def contains(g: ZeroDimensions): Boolean =
    geom.contains(g.geom)

  def within(g: Geometry): Boolean =
    geom.within(g.geom)

  def overlaps(ps: MultiPoint): Boolean =
    geom.overlaps(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiPointSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(l: Line): ZeroDimensionsLineSymDifferenceResult =
    geom.symDifference(l.geom)

  def symDifference(p: Polygon): ZeroDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ls: MultiLine): ZeroDimensionsMultiLineSymDifferenceResult =
    geom.symDifference(ls.geom)

  def symDifference(ps: MultiPolygon): ZeroDimensionsMultiPolygonSymDifferenceResult =
    geom.symDifference(ps.geom)
                         
  // -- Misc.

  def convexHull: Polygon =
    geom.convexHull.asInstanceOf[jts.Polygon]

}
