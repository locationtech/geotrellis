package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom => jts}

object MultiPolygon {
  def apply(ps: Polygon*): MultiPolygon = 
    apply(ps)

  def apply(ps: Traversable[Polygon]): MultiPolygon =
    MultiPolygon(factory.createMultiPolygon(ps.map(_.geom).toArray))
}


case class MultiPolygon(geom: jts.MultiPolygon) extends MultiGeometry 
                                                 with TwoDimensions {

  lazy val area: Double =
    geom.getArea

  lazy val boundary: MultiLineResult =
    geom.getBoundary

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): MultiLineIntersectionResult =
    intersection(l)
  def intersection(l: Line): MultiLineIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): MultiPolygonIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): MultiPolygonIntersectionResult =
    p.intersection(this)

  def &(ls: MultiLine): MultiLineIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiLineIntersectionResult =
    ls.intersection(this)

  def &(ps: MultiPolygon): MultiPolygonIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiPolygonIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): AtMostOneDimensionsMultiPolygonUnionResult =
    union(p)
  def union(p: Point): AtMostOneDimensionsMultiPolygonUnionResult =
    p.union(this)

  def |(l: Line): AtMostOneDimensionsMultiPolygonUnionResult =
    union(l)
  def union(l: Line): AtMostOneDimensionsMultiPolygonUnionResult =
    l.union(this)

  def |(p: Polygon): PolygonPolygonUnionResult =
    union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    p.union(this)

  def |(ps: MultiPoint): AtMostOneDimensionsMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPoint): AtMostOneDimensionsMultiPolygonUnionResult =
    ps.union(this)

  def |(ls: MultiLine) = union(ls)
  def union(ls: MultiLine): AtMostOneDimensionsMultiPolygonUnionResult =
    ls.union(this)

  def |(ps: MultiPolygon): PolygonPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): PolygonPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): MultiPolygonXDifferenceResult =
    difference(p)
  def difference(p: Point): MultiPolygonXDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): MultiPolygonXDifferenceResult =
    difference(l)
  def difference(l: Line): MultiPolygonXDifferenceResult =
    geom.difference(l.geom)

  def -(p: Polygon): PolygonPolygonDifferenceResult =
    difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult =
    geom.difference(p.geom)

  def -(ps: MultiPoint): MultiPolygonXDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): MultiPolygonXDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: MultiLine): MultiPolygonXDifferenceResult =
    difference(ls)
  def difference(ls: MultiLine): MultiPolygonXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    difference(ps)
  def difference(ps: MultiPolygon): PolygonPolygonDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiPolygonSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsMultiPolygonSymDifferenceResult =
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

  def crosses(ps: MultiPoint): Boolean =
    geom.crosses(ps.geom)

  def overlaps(g: TwoDimensions): Boolean =
    geom.crosses(g.geom)

}
