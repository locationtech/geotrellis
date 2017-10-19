/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import org.scalatest._
import matchers._

import geotrellis.vector._

package object testkit {
  object GeometryMatcher {
    def apply[T <: Geometry](geom: T, t: Double, matchFunc: (T,T, Double) => Boolean) =
      new GeometryMatcher[T] {
        val g = geom
        val tolerance = t
        def doMatch(left: T): Boolean = matchFunc(left, g, tolerance)
      }

    def matchPoint(p1: Point, p2: Point, tolerance: Double): Boolean =
      math.abs(p1.x - p2.x) <= tolerance && math.abs(p1.y - p2.y) <= tolerance

    def matchLine(l1: Line, l2: Line, tolerance: Double): Boolean = {
      l1.normalized.points.zip(l2.normalized.points)
        .map { case (p1, p2) => matchPoint(p1, p2, tolerance) }.foldLeft(true)(_ && _)
    }

    def matchPolygon(p1: Polygon, p2: Polygon, tolerance: Double): Boolean = {
      val (np1, np2) = (p1.normalized, p2.normalized)
      matchLine(np1.exterior, np2.exterior, tolerance) &&
      np1.holes.zip(np2.holes)
        .map { case (l1, l2) => matchLine(l1, l2, tolerance) }.foldLeft(true)(_ && _)
    }

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

    def matchGeometry(g1: Geometry, g2: Geometry, tolerance: Double): Boolean =
      g2 match {
        case p: Point =>
          g1.isInstanceOf[Point] && matchPoint(g1.asInstanceOf[Point], p, tolerance)
        case mp: MultiPoint =>
          g1.isInstanceOf[MultiPoint] && matchMultiPoint(g1.asInstanceOf[MultiPoint], mp, tolerance)
        case l: Line =>
          g1.isInstanceOf[Line] && matchLine(g1.asInstanceOf[Line], l, tolerance)
        case ml: MultiLine =>
          g1.isInstanceOf[MultiLine] && matchMultiLine(g1.asInstanceOf[MultiLine], ml, tolerance)
        case p: Polygon =>
          g1.isInstanceOf[Polygon] && matchPolygon(g1.asInstanceOf[Polygon], p, tolerance)
        case mp: MultiPolygon =>
          g1.isInstanceOf[MultiPolygon] && matchMultiPolygon(g1.asInstanceOf[MultiPolygon], mp, tolerance)
        case gc: GeometryCollection =>
          g1.isInstanceOf[GeometryCollection] && matchGeometryCollection(g1.asInstanceOf[GeometryCollection], gc, tolerance)
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

  case class ExtentMatcher(extent: Extent, tolerance: Double) extends Matcher[Extent] {
    def doMatch(left: Extent): Boolean = GeometryMatcher.matchPolygon(left.toPolygon, extent.toPolygon, tolerance)

    def apply(left: Extent) =
      MatchResult(
        doMatch(left),
        s"""$left did not match $extent within tolerance $tolerance """,
        s"""$left matched $extent within tolerance $tolerance"""
      )
  }

  def matchGeom(g: Point): GeometryMatcher[Point] = matchGeom(g, 0.0)
  def matchGeom(g: Point, tolerance: Double): GeometryMatcher[Point] = GeometryMatcher(g, tolerance, GeometryMatcher.matchPoint)

  def matchGeom(g: Line): GeometryMatcher[Line] = matchGeom(g, 0.0)
  def matchGeom(g: Line, tolerance: Double): GeometryMatcher[Line] = GeometryMatcher(g, tolerance, GeometryMatcher.matchLine)

  def matchGeom(g: Polygon): GeometryMatcher[Polygon] = matchGeom(g, 0.0)
  def matchGeom(g: Polygon, tolerance: Double): GeometryMatcher[Polygon] = GeometryMatcher(g, tolerance, GeometryMatcher.matchPolygon)

  def matchGeom(g: MultiPoint): GeometryMatcher[MultiPoint] = matchGeom(g, 0.0)
  def matchGeom(g: MultiPoint, tolerance: Double) = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiPoint)

  def matchGeom(g: MultiLine): GeometryMatcher[MultiLine] = matchGeom(g, 0.0)
  def matchGeom(g: MultiLine, tolerance: Double): GeometryMatcher[MultiLine] = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiLine)

  def matchGeom(g: MultiPolygon): GeometryMatcher[MultiPolygon] = matchGeom(g, 0.0)
  def matchGeom(g: MultiPolygon, tolerance: Double): GeometryMatcher[MultiPolygon] = GeometryMatcher(g, tolerance, GeometryMatcher.matchMultiPolygon)

  def matchGeom(g: GeometryCollection): GeometryMatcher[GeometryCollection] = matchGeom(g, 0.0)
  def matchGeom(g: GeometryCollection, tolerance: Double): GeometryMatcher[GeometryCollection] = GeometryMatcher(g, tolerance, GeometryMatcher.matchGeometryCollection)

  def matchGeom(g: Extent): ExtentMatcher = matchGeom(g, 0.0)
  def matchGeom(g: Extent, tolerance: Double): ExtentMatcher = ExtentMatcher(g, tolerance)

  def matchGeom(g: Geometry): GeometryMatcher[Geometry] = matchGeom(g, 0.0)
  def matchGeom(g: Geometry, tolerance: Double): GeometryMatcher[Geometry] =
    GeometryMatcher(g, tolerance, GeometryMatcher.matchGeometry)
}
