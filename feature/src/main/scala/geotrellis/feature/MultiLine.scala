package geotrellis.feature

import GeomFactory._

import com.vividsolutions.jts.{geom=>jts}

object MultiLine {
  def apply(ls: Line*): MultiLine = 
    MultiLine(ls)

  def apply(ls: Traversable[Line])(implicit d: DummyImplicit): MultiLine = 
    MultiLine(factory.createMultiLineString(ls.map(_.geom).toArray))
}

case class MultiLine(geom: jts.MultiLineString) extends MultiGeometry 
                                                 with TwoDimensions {

  lazy val isClosed: Boolean =
    geom.isClosed

  lazy val boundary: LineBoundaryResult =
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

  def &(p: Polygon): MultiLineIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): MultiLineIntersectionResult =
    p.intersection(this)

  def &(ps: MultiPoint): MultiPointIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPoint): MultiPointIntersectionResult =
    ps.intersection(this)

  def &(ls: MultiLine): MultiLineIntersectionResult =
    intersection(ls)
  def intersection(ls: MultiLine): MultiLineIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: MultiPolygon): MultiLineIntersectionResult =
    intersection(ps)
  def intersection(ps: MultiPolygon): MultiLineIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): PointMultiLineUnionResult =
    union(p)
  def union(p: Point): PointMultiLineUnionResult =
    p.union(this)

  def |(l: Line): LineLineUnionResult =
    union(l)
  def union(l:Line): LineLineUnionResult =
    l.union(this)

  def |(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    geom.union(p.geom)

  def |(ps: MultiPoint): PointMultiLineUnionResult =
    union(ps)
  def union(ps: MultiPoint): PointMultiLineUnionResult =
    ps.union(this)

  def |(ls: MultiLine): LineLineUnionResult =
    union(ls)
  def union(ls: MultiLine): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
    union(ps)
  def union(ps: MultiPolygon): AtMostOneDimensionsMultiPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): MultiLinePointDifferenceResult =
    difference(p)
  def difference(p: Point): MultiLinePointDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): LineXDifferenceResult =
    difference(l)
  def difference(l: Line): LineXDifferenceResult = 
    geom.difference(l.geom)
  
  def -(p: Polygon): LineXDifferenceResult =
    difference(p)
  def difference(p: Polygon): LineXDifferenceResult = 
    geom.difference(p.geom)
  
  def -(ps: MultiPoint): MultiLinePointDifferenceResult =
    difference(ps)
  def difference(ps: MultiPoint): MultiLinePointDifferenceResult = 
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

  def symDifference(g: ZeroDimensions): ZeroDimensionsMultiLineSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(p: Polygon): OneDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ps: MultiPolygon): OneDimensionsMultiPolygonSymDifferenceResult =
    geom.symDifference(ps.geom)

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
