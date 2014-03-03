package geotrellis.feature

import com.vividsolutions.jts.{geom=>jts}
import GeomFactory._
import com.vividsolutions.jts.geom.MultiLineString

object LineSet {
  def apply(ls: Line*): LineSet = 
    LineSet(ls.toSet)
  def apply(ls: Seq[Line])(implicit d: DummyImplicit): LineSet = 
    LineSet(ls.toSet)
}

case class LineSet(ls: Set[Line]) extends GeometrySet 
                                     with TwoDimensions {

  val geom: MultiLineString =
    factory.createMultiLineString(ls.map(_.geom).toArray)

  lazy val isClosed: Boolean =
    geom.isClosed

  lazy val boundary: OneDimensionsBoundaryResult =
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

  def &(p: Polygon): LineSetIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): LineSetIntersectionResult =
    p.intersection(this)

  def &(ps: PointSet): PointSetIntersectionResult =
    intersection(ps)
  def intersection(ps: PointSet): PointSetIntersectionResult =
    ps.intersection(this)

  def &(ls: LineSet): LineSetIntersectionResult =
    intersection(ls)
  def intersection(ls: LineSet): LineSetIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet): LineSetIntersectionResult =
    intersection(ps)
  def intersection(ps: PolygonSet): LineSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(p: Point): PointLineSetUnionResult =
    union(p)
  def union(p: Point): PointLineSetUnionResult =
    p.union(this)

  def |(l: Line): LineLineUnionResult =
    union(l)
  def union(l:Line): LineLineUnionResult =
    l.union(this)

  def |(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    union(p)
  def union(p: Polygon): AtMostOneDimensionsPolygonUnionResult =
    geom.union(p.geom)

  def |(ps: PointSet): PointLineSetUnionResult =
    union(ps)
  def union(ps: PointSet): PointLineSetUnionResult =
    ps.union(this)

  def |(ls: LineSet): LineLineUnionResult =
    union(ls)
  def union(ls: LineSet): LineLineUnionResult =
    geom.union(ls.geom)

  def |(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    union(ps)
  def union(ps: PolygonSet): AtMostOneDimensionsPolygonSetUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): LineSetPointDifferenceResult =
    difference(p)
  def difference(p: Point): LineSetPointDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): LineXDifferenceResult =
    difference(l)
  def difference(l: Line): LineXDifferenceResult = 
    geom.difference(l.geom)
  
  def -(p: Polygon): LineXDifferenceResult =
    difference(p)
  def difference(p: Polygon): LineXDifferenceResult = 
    geom.difference(p.geom)
  
  def -(ps: PointSet): LineSetPointDifferenceResult =
    difference(ps)
  def difference(ps: PointSet): LineSetPointDifferenceResult = 
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

  def symDifference(g: ZeroDimensions): ZeroDimensionsLineSetSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(p: Polygon): OneDimensionsPolygonSymDifferenceResult =
    geom.symDifference(p.geom)

  def symDifference(ps: PolygonSet): OneDimensionsPolygonSetSymDifferenceResult =
    geom.symDifference(ps.geom)

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
