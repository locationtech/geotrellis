package geotrellis.feature

import com.vividsolutions.jts.{geom => jts}
import GeomFactory._

object Polygon {

  implicit def jtsToPolygon(geom: jts.Polygon): Polygon =
    Polygon(geom)

  def apply(exterior: Line): Polygon =
    apply(exterior, Set())

  def apply(exterior: Line, holes:Set[Line]): Polygon = {
    if(!exterior.isClosed) {
      sys.error(s"Cannot create a polygon with unclosed exterior: $exterior")
    }

    if(exterior.points.length < 4) {
      sys.error(s"Cannot create a polygon with exterior with less that 4 points: $exterior")
    }

    val extGeom = factory.createLinearRing(exterior.geom.getCoordinates)

    val holeGeoms = (
      for (hole <- holes) yield {
        if (!hole.isClosed) {
          sys.error(s"Cannot create a polygon with an unclosed hole: $hole")
        } else {
          if (hole.points.length < 4)
            sys.error(s"Cannot create a polygon with a hole with less that 4 points: $hole")
          else
            factory.createLinearRing(hole.geom.getCoordinates)
        }
      }).toArray

    factory.createPolygon(extGeom, holeGeoms)
  }

}

case class Polygon(geom: jts.Polygon) extends Geometry 
                                         with TwoDimensions {

  assert(!geom.isEmpty)

  lazy val isRectangle: Boolean =
    geom.isRectangle

  lazy val area: Double =
    geom.getArea

  lazy val exterior: Line =
    Line(geom.getExteriorRing)

  lazy val boundary: PolygonBoundaryResult =
    geom.getBoundary

  lazy val vertices: PointSet =
    geom.getCoordinates

  lazy val boundingBox: Option[Polygon] =
    if (geom.isEmpty) None else Some(geom.getEnvelope.asInstanceOf[Polygon])

  lazy val perimeter: Double =
    geom.getLength

  // -- Intersection

  def &(p: Point): PointGeometryIntersectionResult =
    intersection(p)
  def intersection(p: Point): PointGeometryIntersectionResult =
    p.intersection(this)

  def &(l: Line): OneDimensionsAtLeastOneDimensionsIntersectionResult =
    intersection(l)
  def intersection(l: Line): OneDimensionsAtLeastOneDimensionsIntersectionResult =
    l.intersection(this)

  def &(p: Polygon): PolygonPolygonIntersectionResult =
    intersection(p)
  def intersection(p: Polygon): PolygonPolygonIntersectionResult =
    geom.intersection(p.geom)

  def &(ps: PointSet): PointSetGeometryIntersectionResult =
    intersection(ps)
  def intersection(ps: PointSet): PointSetGeometryIntersectionResult =
    geom.intersection(ps.geom)

  def &(ls: LineSet): OneDimensionsAtLeastOneDimensionsIntersectionResult =
    intersection(ls)
  def intersection(ls: LineSet): OneDimensionsAtLeastOneDimensionsIntersectionResult =
    geom.intersection(ls.geom)

  def &(ps: PolygonSet): PolygonSetIntersectionResult =
    intersection(ps)
  def intersection(ps: PolygonSet): PolygonSetIntersectionResult =
    geom.intersection(ps.geom)

  // -- Union

  def |(g: AtMostOneDimensions): AtMostOneDimensionsPolygonUnionResult =
    union(g)
  def union(g: AtMostOneDimensions): AtMostOneDimensionsPolygonUnionResult =
    geom.union(g.geom)

  def |(p:Polygon): PolygonPolygonUnionResult =
    union(p)
  def union(p: Polygon): PolygonPolygonUnionResult =
    geom.union(p.geom)

  def |(ps: PolygonSet): PolygonPolygonUnionResult =
    union(ps)
  def union(ps: PolygonSet): PolygonPolygonUnionResult =
    geom.union(ps.geom)

  // -- Difference

  def -(p: Point): PolygonXDifferenceResult =
    difference(p)
  def difference(p: Point): PolygonXDifferenceResult =
    geom.difference(p.geom)

  def -(l: Line): PolygonXDifferenceResult =
    difference(l)
  def difference(l: Line): PolygonXDifferenceResult =
    geom.difference(l.geom)

  def -(p: Polygon): PolygonPolygonDifferenceResult =
    difference(p)
  def difference(p: Polygon): PolygonPolygonDifferenceResult =
    geom.difference(p.geom)

  def -(ps: PointSet): PolygonXDifferenceResult =
    difference(ps)
  def difference(ps: PointSet): PolygonXDifferenceResult =
    geom.difference(ps.geom)

  def -(ls: LineSet): PolygonXDifferenceResult =
    difference(ls)
  def difference(ls: LineSet): PolygonXDifferenceResult =
    geom.difference(ls.geom)

  def -(ps: PolygonSet): PolygonPolygonDifferenceResult =
    difference(ps)
  def difference(ps: PolygonSet): PolygonPolygonDifferenceResult =
    geom.difference(ps.geom)

  // -- SymDifference

  def symDifference(g: ZeroDimensions): ZeroDimensionsPolygonSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: OneDimensions): OneDimensionsPolygonSymDifferenceResult =
    geom.symDifference(g.geom)

  def symDifference(g: TwoDimensions): TwoDimensionsSymDifferenceResult =
    geom.symDifference(g.geom)

  // -- Buffer

  def buffer(d: Double): Polygon =
    geom.buffer(d).asInstanceOf[Polygon]

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
    geom.overlaps(g.geom)

}
