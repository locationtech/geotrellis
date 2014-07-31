package geotrellis.testkit

import org.scalatest._
import matchers._

import geotrellis.vector._

package object vector {
  object GeometryMatcher {
    def apply[T <: Geometry](geom: T, t: Double, matchFunc: (T,T, Double) => Boolean) =
      new GeometryMatcher[T] { 
        val g = geom
        val tolerance = t
        def doMatch(left: T): Boolean = matchFunc(left, g, tolerance)
      }

    def matchPoint(p1: Point, p2: Point, tolerance: Double): Boolean =
      math.abs(p1.x - p2.x) < tolerance && math.abs(p1.y - p2.y) < tolerance

    def matchLine(l1: Line, l2: Line, tolerance: Double): Boolean =
      l1.normalized.points.zip(l2.normalized.points)
        .map { case (p1, p2) => matchPoint(p1, p2, tolerance) }.foldLeft(true)(_ && _)

    def matchPolygon(p1: Polygon, p2: Polygon, tolerance: Double): Boolean =
      matchLine(p1.exterior, p2.exterior, tolerance) &&
      p1.normalized.holes.zip(p2.normalized.holes)
        .map { case (l1, l2) => matchLine(l1, l2, tolerance) }.foldLeft(true)(_ && _)

    def matchMultiPoint(mp1: MultiPoint, mp2: MultiPoint, tolerance: Double): Boolean =
      mp1.normalized.points.zip(mp2.normalized.points)
         .map { case (p1, p2) => matchPoint(p1, p2, tolerance) }.foldLeft(true)(_ && _)

    def matchMultiLine(ml1: MultiLine, ml2: MultiLine, tolerance: Double): Boolean =
      ml1.normalized.lines.zip(ml2.normalized.lines)
         .map { case (l1, l2) => matchLine(l1, l2, tolerance) }.foldLeft(true)(_ && _)


    def matchMultiPolygon(mp1: MultiPolygon, mp2: MultiPolygon, tolerance: Double): Boolean =
      mp1.normalized.polygons.zip(mp2.normalized.polygons)
         .map { case (p1, p2) => matchPolygon(p1, p2, tolerance) }.foldLeft(true)(_ && _)

    def matchGeometryCollection(gc1: GeometryCollection, gc2: GeometryCollection, tolerance: Double): Boolean = {
      val ngc1 = gc1.normalized
      val ngc2 = gc2.normalized
      ngc1.points.zip(ngc2.points).map { case (p1, p2) => matchPoint(p1, p2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.lines.zip(ngc2.lines).map { case (l1, l2) => matchLine(l1, l2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.polygons.zip(ngc2.polygons).map { case (p1, p2) => matchPolygon(p1, p2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.multiPoints.zip(ngc2.multiPoints).map { case (mp1, mp2) => matchMultiPoint(mp1, mp2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.multiLines.zip(ngc2.multiLines).map { case (ml1, ml2) => matchMultiLine(ml1, ml2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.multiPolygons.zip(ngc2.multiPolygons).map { case (mp1, mp2) => matchMultiPolygon(mp1, mp2, tolerance) }.foldLeft(true)(_ && _) &&
      ngc1.geometryCollections.zip(ngc2.geometryCollections).map { case (gc1, gc2) => matchGeometryCollection(gc1, gc2, tolerance) }.foldLeft(true)(_ && _)
    }

  }

  trait GeometryMatcher[T <: Geometry] extends Matcher[T] {
    val g: T
    val tolerance:Double
    def doMatch(left: T): Boolean

    def apply(left: T) = 
      MatchResult(
        doMatch(left),
        s"""$left did not match $g within tolerance $tolerance """,
        s"""$left matched $g within tolerance $tolerance"""
      )
  }


  def matchGeom(g: Point, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchPoint)
  def matchGeom(g: Line, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchLine)
  def matchGeom(g: Polygon, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchPolygon)
  def matchGeom(g: MultiPoint, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiPoint)
  def matchGeom(g: MultiLine, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiLine)
  def matchGeom(g: MultiPolygon, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiPolygon)
  def matchGeom(g: GeometryCollection, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchGeometryCollection)
}
