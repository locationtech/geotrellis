package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object PolygonSet {
  def apply(ps: Polygon*): PolygonSet = 
    apply(ps)

  def apply(ps: Traversable[Polygon]): PolygonSet =
    PolygonSet(factory.createMultiPolygon(ps.map(_.geom).toArray))
}


case class PolygonSet(geom: jts.MultiPolygon) extends GeometrySet 
                                                 with TwoDimensions {

  lazy val area: Double =
    geom.getArea

  lazy val boundary: LineSetResult =
    geom.getBoundary

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): LineSetIntersectionResult =
    intersection(l)
  def intersection(l: Line): LineSetIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): PolygonSetIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): PolygonSetIntersectionResult =
    p.intersection(this)

  def &(ls: LineSet): LineSetIntersectionResult =
    intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    ls.intersection(this)

  def &(ps: PolygonSet): PolygonSetIntersectionResult =
    intersection(ps)
  def intersection(ps: PolygonSet): PolygonSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): AtMostOneDimensionsPolygonSetUnionResult =
    union(p)
  def union(p: Point): AtMostOneDimensionsPolygonSetUnionResult =
    p.union(this)

  def |(l: Line): AtMostOneDimensionsPolygonSetUnionResult =
    union(l)
  def union(l: Line): AtMostOneDimensionsPolygonSetUnionResult =
    l.union(this)

  def |(p: Polygon): PolygonPolygonUnionResult =
    union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    p.union(this)

  def |(ps: PointSet): AtMostOneDimensionsPolygonSetUnionResult =
    union(ps)
  def union(ps: PointSet): AtMostOneDimensionsPolygonSetUnionResult =
    ps.union(this)

  def |(ls: LineSet) = union(ls)
  def union(ls: LineSet): AtMostOneDimensionsPolygonSetUnionResult =
    ls.union(this)

  def |(ps: PolygonSet): PolygonPolygonUnionResult =
    union(ps)
  def union(ps: PolygonSet): PolygonPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): PolygonSetXDifferenceResult =
    difference(p)
  def difference(p: Point): PolygonSetXDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): PolygonSetXDifferenceResult =
    difference(l)
  def difference(l: Line): PolygonSetXDifferenceResult =
    geom.difference(l.geom)

  def -(p: Polygon): PolygonPolygonDifferenceResult =
    difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult =
    geom.difference(p.geom)

  def -(ps: PointSet): PolygonSetXDifferenceResult =
    difference(ps)
  def difference(ps: PointSet): PolygonSetXDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: LineSet): PolygonSetXDifferenceResult =
    difference(ls)
  def difference(ls: LineSet): PolygonSetXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: PolygonSet): PolygonPolygonDifferenceResult =
    difference(ps)
  def difference(ps: PolygonSet): PolygonPolygonDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: TwoDimensions): TwoDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  // -- Predicates

  def contains(g: Geometry): Boolean =
    geom.contains(g.geom)

  def within(g: TwoDimensions): Boolean =
    geom.within(g.geom)

  def crosses(g: OneDimensions): Boolean =
    geom.crosses(g.geom)

  def crosses(ps: PointSet): Boolean =
    geom.crosses(ps.geom)

  def overlaps(g: TwoDimensions): Boolean =
    geom.crosses(g.geom)

}
