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

import org.locationtech.jts.{geom => jts}
import GeomFactory._

import org.scalatest._

class PointSpec extends FunSpec with Matchers {
  describe("Point") {

    it ("should return true for comparing points with equal x and y") {
      val x = 123.321
      val y = -0.4343434
      Point(x,y) should be (Point(x,y))
    }

    // -- Intersection

    it ("should intersect with another Geometry and return a PointResult") {
      val p = Point(1,1)
      val l = Line(Point(0, 0), Point(2,2))
      val result = p & l
      result should be (PointResult(Point(1, 1)))
    }

    it ("should return a NoResult if it does not intersect with another Geometry")  {
      val p = Point(1,1)
      val l = Line(Point(0, 0), Point(0, 2))
      val result = p & l
      result should be (NoResult)
    }

    // -- Union

    it ("should union with a ZeroDimensions and return a PointResult") {
      val p = Point(1,1)
      val zd = Seq[Point](Point(1,1))
      val result = p | zd
      result should be (PointResult(Point(1,1)))
    }

    it ("should union with a ZeroDimensions and return a MultiPointResult") {
      val p = Point(1,1)
      val zd = Seq[Point](Point(1,1), Point(2,2))
      val result = p | zd
      result should be (MultiPointResult(Seq[Point](Point(1,1), Point(2,2))))
    }

    it ("should union with a Line and return a LineResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p | l
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should union with a Line and return a GeometryCollectionResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(0,2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(1,1)), lines = Seq(Line(Point(0,0), Point(0,2))))
      val result = p | l
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a Polygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt | poly
      result should be (PolygonResult(poly))
    }

    it ("should union with a Polygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(pt), polygons = Seq(poly))
      val result = pt | poly
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with an empty MultiLine and return a PointResult") {
      val p = Point(1,1)
      val ls = MultiLine(Seq())
      val result = p | ls
      result should be (PointResult(p))
    }

    it ("should union with a MultiLine and return a LineResult") {
      val p = Point(1,1)
      val ls = MultiLine(Seq(Line(Point(0,0), Point(2,2))))
      val result = p | ls
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should union with a MultiLine and return a MultiLineResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,5), Point(5,5))
      val ls = MultiLine(Seq(l1, l2))
      val result = p | ls
      result should be (MultiLineResult(Seq(l1, l2)))
    }

    it ("should union with a MultiLine and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,5), Point(5,5))
      val ls = MultiLine(Seq(l1, l2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(p), lines = Seq(l1, l2))
      val result = p | ls
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a MultiPolygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val polySet = MultiPolygon(Seq(poly))
      val result = pt | polySet
      result should be (PolygonResult(poly))
    }

    it ("should union with a MultiPolygon and return a MultiPolygonResult") {
      val pt = Point(1,1)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = MultiPolygon(Seq(poly1, poly2))
      val result = pt | polySet
      result should be (MultiPolygonResult(Seq(poly1, poly2)))
    }

    it ("should union with a MultiPolygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = MultiPolygon(Seq(poly1, poly2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(pt), polygons = Seq(poly1, poly2))
      val result = pt | polySet
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with an empty MultiPolygon and return a PointResult") {
      val p = Point(1,1)
      val mp = MultiPolygon(Seq())
      p | mp should be (PointResult(p))
    }

    // -- Difference

    it ("should difference with another Geometry and return a PointResult") {
      val p = Point(1,1)
      val l = Line(Point(5,5), Point(11, 4))
      val result = p - l
      result should be (PointResult(Point(1,1)))
    }

    it ("should difference with another Geometry and return a NoResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt - poly
      result should be (NoResult)
    }

    // -- symDifference

    it ("should symDifference with a Point and return a MultiPointResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val result = p1.symDifference(p2);
      result should be (MultiPointResult(Seq(p2, p1)))
    }

    it ("should symDifference with a Point and return a NoResult") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.symDifference(p2);
      result should be (NoResult)
    }

    it ("should symDifference with a Line and return a LineResult") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p.symDifference(l)
      result should be (LineResult(l))
    }

    it ("should symDifference with a Line and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l = Line(Point(0,0), Point(2,2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(p), lines = Seq(l))
      val result = p | l
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a Polygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val result = pt.symDifference(poly)
      result should be (PolygonResult(poly))
    }

    it ("should symDifference with a Polygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(pt), polygons = Seq(poly))
      val result = pt | poly
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a MultiPoint and return a PointResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val p3 = Point(1,1)
      val ps = MultiPoint(Seq(p2, p3))
      val result = p1.symDifference(ps)
      result should be (PointResult(p2))
    }

    it ("should symDifference with a MultiPoint and return a MultiPointResult") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val p3 = Point(3,3)
      val ps = MultiPoint(Seq(p2, p3))
      val result = p1.symDifference(ps)
      result should be (MultiPointResult(Seq(p1, p2, p3)))
    }

    it ("should symDifference with a MultiPoint and return a NoResult") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val p3 = Point(1,1)
      val ps = MultiPoint(Seq(p2, p3))
      val result = p1.symDifference(ps)
      result should be (NoResult)
    }

    it ("should symDifference with an empty MultiLine and return a PointResult") {
      val p = Point(1,1)
      val ml = MultiLine(Seq())
      val result = p.symDifference(ml)
      result should be (PointResult(p))
    }

    it ("should symDifference with a MultiLine and return a LineResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val ls = MultiLine(Seq(l1))
      val result = p.symDifference(ls)
      result should be (LineResult(Line(Point(0,0), Point(2,2))))
    }

    it ("should symDifference with a MultiLine and return a MultiLineResult") {
      val p = Point(1,1)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,0), Point(2,0))
      val ls = MultiLine(Seq(l1, l2))
      val result = p.symDifference(ls)
      result should be (MultiLineResult(Seq(l1, l2)))
    }

    it ("should symDifference with a MultiLine and return a GeometryCollectionResult") {
      val p = Point(11,11)
      val l1 = Line(Point(0,0), Point(2,2))
      val l2 = Line(Point(0,0), Point(0,2))
      val ls = MultiLine(Seq(l1, l2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(p), lines = Seq(l1, l2))
      val result = p.symDifference(ls)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with an empty MultiPolygon and return a PointResult") {
      val p = Point(1,1)
      val mp = MultiPolygon(Seq())
      val result = p.symDifference(mp)
      result should be (PointResult(p))
    }

    it ("should symDifference with a MultiPolygon and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val polySet = MultiPolygon(Seq(poly))
      val result = pt.symDifference(polySet)
      result should be (PolygonResult(poly))
    }

    it ("should symDifference with a MultiPolygon and return a MultiPolygonResult") {
      val pt = Point(1,1)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = MultiPolygon(Seq(poly1, poly2))
      val result = pt.symDifference(polySet)
      result should be (MultiPolygonResult(Seq(poly1, poly2)))
    }

    it ("should symDifference with a MultiPolygon and return a GeometryCollectionResult") {
      val pt = Point(11,11)
      val poly1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val poly2 = Polygon(Line(Point(10,0), Point(10,2), Point(12,2), Point(12,0), Point(10,0)))
      val polySet = MultiPolygon(Seq(poly1, poly2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(pt), polygons = Seq(poly1, poly2))
      val result = pt.symDifference(polySet)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Buffer

    it ("should buffer and return a Polygon") {
      val p = Point(1,1)
      val result = p.buffer(1)
      result match {
        case Polygon(_) => // expected
        case _ => fail()
      }
    }

    // -- Predicates

    it ("should contain another point with the same coordinates") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.contains(p2)
      result should be (true)
    }

    it ("should contain a Point with the same coordinates") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val result = p1.contains(p2)
      result should be (true)
    }

    it ("should contain a MultiPoint having all points with the same coordinates as this Point") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val p3 = Point(1,1)
      val ps = MultiPoint(Seq(p2, p3))
      val result = p1.contains(ps)
      result should be (true)
    }

    it ("should be within another intersecting Geometry") {
      val p = Point(1,1)
      val l = Line(Point(0,0), Point(2,2))
      val result = p.within(l)
      result should be (true)
    }

    it ("should be covered by a Point") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      p1.coveredBy(p2) should be (true)
    }

    it ("should cover a MultiPoint") {
      val p1 = Point(1,1)
      val p2 = Point(1,1)
      val p3 = Point(1,1)
      val mp = MultiPoint(p2, p3)
      p1.covers(mp) should be (true)
    }

    it ("should touch a Polygon") {
      val p = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,1), Point(1,1), Point(1,0), Point(0,0)))
    }

  }

}
