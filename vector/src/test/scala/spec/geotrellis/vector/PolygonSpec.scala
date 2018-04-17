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

import geotrellis.vector.testkit._

import org.locationtech.jts.{geom=>jts}

import org.scalatest._

class PolygonSpec extends FunSpec with Matchers {
  describe("Polygon") {
    it("should be a closed Polygon if constructed with l(0) == l(-1)") {
      val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0))))
      p.exterior.isClosed should be (true)
    }

    it("should throw if attempt to construct with unclosed line") {
      intercept[Exception] {
        val p = Polygon(Line(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1))))
      }
    }

    it ("should be a rectangle if constructed with a rectangular Seq of points") {
      val p = Polygon(Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0)))
      p.isRectangle should be (true)
    }

    it ("should have an area") {
      val p = Polygon(Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0)))
      p.area should be (25)
    }

    it ("should have an exterior") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p.exterior should be (l)
    }

    it ("should have a boundary that is a LineResult") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p.boundary should be (LineResult(l))
    }

    it ("should have a boundary that is a MultiLineResult") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val h = Line(Point(1,1), Point(1,4), Point(4,4), Point(4,1), Point(1,1))
      val p = Polygon(l, Seq(h))
      p.boundary should be (MultiLineResult(Seq(l, h)))
    }

    it ("should have vertices") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p.vertices should be (Array(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0)))
    }

    it ("should have an envelope whose points are " +
        "(minx, miny), (minx, maxy), (maxx, maxy), (maxx, miny), (minx, miny).") {
      val l = Line(Point(0,0), Point(2,2), Point(4,0), Point(0,0))
      val p = Polygon(l)
      p.envelope should be (Extent(0,0,4,2))
    }

    it ("should have a perimeter equal to the length of its boundary") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val h = Line(Point(1,1), Point(1,4), Point(4,4), Point(4,1), Point(1,1))
      val p = Polygon(l, Seq(h))
      p.perimeter should be (20 + 12)
    }

    it ("should intersect with a MultiPoint and return a NoResult") {
      val p1 = Point(10,10)
      val p2 = Point(11,11)
      val mp = MultiPoint(p1, p2)
      val l = Line(Point(0,0), Point(2,2), Point(4,0), Point(0,0))
      val p = Polygon(l)
      p & mp should be (NoResult)
    }

    it ("should intersect with a MultiPoint and return a PointResult") {
      val p1 = Point(10,10)
      val p2 = Point(5,5)
      val mp = MultiPoint(p1, p2)
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p & mp should be (PointResult(p2))
    }

    it ("should intersect with a MultiPoint and return a MultiPointResult") {
      val p1 = Point(1,1)
      val p2 = Point(4,4)
      val mp = MultiPoint(p1, p2)
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p & mp should be (MultiPointResult(Seq(p1, p2)))
    }

    it ("should intersect with a TwoDimensions and return a NoResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(6,0), Point(7,0), Point(7,1), Point(6,1), Point(6,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (NoResult)
    }

    it ("should intersect with a TwoDimensions and return a PointResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(5,0), Point(7,0), Point(7,1), Point(6,1), Point(5,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (PointResult(Point(5,0)))
    }

    it ("should intersect with a TwoDimensions and return a LineResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(5,0), Point(7,0), Point(7,1), Point(5,1), Point(5,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (LineResult(Line(Point(5,1), Point(5,0))))
    }

    it ("should intersect with a TwoDimensions and return a PolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(4,0), Point(7,0), Point(7,1), Point(4,1), Point(4,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (PolygonResult(Polygon(Line(Point(5,1), Point(5,0),
                                                    Point(4,0), Point(4,1), Point(5,1)))))
    }

    it ("should intersect with a TwoDimensions and return a MultiPointResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(5,0), Point(7,0), Point(5,5), Point(10,5), Point(10,-1), Point(5,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (MultiPointResult(Seq(Point(5,0), Point(5,5))))
    }

    it ("should intersect with a TwoDimensions and return a MultiLineResult") {
      val l1 = Line(Point(0,0), Point(0,6), Point(5,6), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(5,0), Point(5,2), Point(6,2), Point(6,4), Point(5,4),
                    Point(5,6), Point(7,6), Point(7,0), Point(5,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (MultiLineResult(Seq(Line(Point(5,6), Point(5,4)),
                                             Line(Point(5,2), Point(5,0)))))
    }

    it ("should intersect with a TwoDimensions and return a MultiPolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(3,4),
                    Point(3,6), Point(2,6), Point(2,4), Point(0,4))
      val p2 = Polygon(l2)
      p1 & p2 should be (MultiPolygonResult(Seq(Polygon(Line(Point(0,4), Point(0,5),
                                                             Point(2,5), Point(2,4), Point(0,4))),
                                                Polygon(Line(Point(3,5), Point(5,5),
                                                             Point(5,4), Point(3,4), Point(3,5))))))
    }

    it ("should intersect with a TwoDimensions and return a GeometryCollectionResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,5), Point(2,6),
                    Point(2,4), Point(0,4))
      val p2 = Polygon(l2)
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(5,5)), polygons = Seq(Polygon(Line(Point(0,4), Point(0,5),
          Point(2,5), Point(2,4), Point(0,4)))))
      val result = p1 & p2
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Union

    it ("should union with a Polygon and return a PolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(0,4))
      val p2 = Polygon(l2)
      p1 | p2 should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,4), Point(0,5), Point(0,7),
                                                    Point(5,7), Point(5,5), Point(5,4), Point(5,0),
                                                    Point(0,0)))))
    }

    it ("should union with a Polygon and return a MultiPolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,6), Point(0,7), Point(5,7), Point(5,6), Point(0,6))
      val p2 = Polygon(l2)
      p1 | p2 should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it ("should union with a MultiPolygon and return a PolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val mp = {
        val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(0,4))
        require(l2.isValid)
        val l3 = Line(Point(0,3), Point(2,3), Point(2,0), Point(0,3))
        require(l3.isValid)
        val p2 = Polygon(l2)
        require(p2.isValid)
        val p3 = Polygon(l3)
        require(p3.isValid)

        MultiPolygon(p2, p3)
      }

      require(p1.isValid)
      require(mp.isValid)
      p1 | mp should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,4), Point(0,5), Point(0,7),
        Point(5,7), Point(5,5), Point(5,4), Point(5,0),
        Point(0,0)))))
    }

    it ("should make an invalid MultiPolygon into a Polygon") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(0,4))
      val p2 = Polygon(l2)
      val mp = MultiPolygon(Polygon(l1), Polygon(l2))
      (mp.isValid) should be (false)
      val mpUnion = mp.union match {
        case PolygonResult(p) => p
        case MultiPolygonResult(mp) => mp
        case NoResult => MultiPolygon.EMPTY
      }
      (mpUnion.isValid) should be (true)
      mpUnion should be (Polygon(Line(Point(0, 0), Point(0, 4), Point(0, 5), Point(0, 7),
        Point(5, 7), Point(5, 5), Point(5, 4), Point(5, 0), Point(0, 0))))
    }

    // -- Cascaded Polygon Union (mimics the binary union tests)
    it ("should cascade union with a Polygon and return a PolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      require(p1.isValid)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(0,4))
      val p2 = Polygon(l2)
      require(p2.isValid)
      val geomList = List(p1, p2)
      geomList.unionGeometries should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,4),
        Point(0,5), Point(0,7), Point(5,7), Point(5,5), Point(5,4), Point(5,0),
        Point(0,0)))))
    }

    it ("should cascade union with a Polygon and return a MultiPolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,6), Point(0,7), Point(5,7), Point(5,6), Point(0,6))
      val p2 = Polygon(l2)
      val geomList = List(p1, p2)
      geomList.unionGeometries should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it ("should cascade union with a MultiPolygon and return a PolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      require(p1.isValid)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(0,4))
      val l3 = Line(Point(0,3), Point(2,3), Point(2,0), Point(0,3))
      val mp = MultiPolygon(Polygon(l2), Polygon(l3))
      require(mp.isValid)
      val geomList = List(MultiPolygon(p1), mp)
      geomList.unionGeometries should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,4),
        Point(0,5), Point(0,7), Point(5,7), Point(5,5), Point(5,4), Point(5,0),
        Point(0,0)))))
    }

    // -- Difference

    it ("should difference with a Point and return a PolygonResult") {
      val pt = Point(1,1)
      val poly = Polygon(Line(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      poly - pt should be (PolygonResult(poly))
    }

    it ("should difference with a MultiPoint and return a PolygonResult") {
      val pt1 = Point(0,0)
      val pt2 = Point(0,4)
      val pt3 = Point(4,4)
      val pt4 = Point(4,0)
      val mp = MultiPoint(pt1, pt2, pt3, pt4)
      val poly = Polygon(Line(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      poly - mp should be (PolygonResult(poly))
    }

    it ("should difference with a Line that matches its exterior and return a PolygonResult") {
      val pt1 = Point(0,0)
      val pt2 = Point(0,4)
      val pt3 = Point(4,4)
      val pt4 = Point(4,0)
      val l = Line(pt1, pt2, pt3, pt4, pt1)
      val poly = Polygon(Line(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l should be (poly.exterior)
      poly - l should be (PolygonResult(poly))
    }

    it ("should difference with a Line and return a PolygonResult") {
      val pt1 = Point(-2,-2)
      val pt2 = Point(0,0)
      val pt3 = Point(2,2)
      val pt4 = Point(4,4)
      val pt5 = Point(6,6)
      val l = Line(pt1, pt2, pt3, pt4, pt5)
      val poly = Polygon(Line(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      poly - l should be (PolygonResult(poly))
    }

    it ("should difference with a MultiLine and return a PolygonResult") {
      val pt1 = Point(0,0)
      val pt2 = Point(0,4)
      val pt3 = Point(4,4)
      val pt4 = Point(4,0)
      val l1 = Line(pt1, pt3)
      val l2 = Line(pt2, pt4)
      val ml = MultiLine(l1, l2)
      val poly = Polygon(Line(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      poly - ml should be (PolygonResult(poly))
    }

    it ("should difference with a Polygon and return a PolygonResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(2,12), Point(2,18), Point(8,18), Point(8,12), Point(2,12)))
      p1 - p2 should be (PolygonResult(p1))
    }

    it ("should difference with a Polygon and return a MultiPolygonResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(-2,4), Point(-2,6), Point(12,6), Point(12,4), Point(-2,4)))
      val p3 = Polygon(Line(Point(0,6), Point(0,10), Point(10,10), Point(10,6), Point(0,6)))
      val p4 = Polygon(Line(Point(0,0), Point(0,4), Point(10,4), Point(10,0), Point(0,0)))
      p1 - p2 should be (MultiPolygonResult(Seq(p3, p4)))
    }

    // -- SymDifference

    it ("should symDifference with a MultiPoint and return a PolygonResult") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val mp = MultiPoint(Seq(Point(5,5), Point(6,6)))
      p.symDifference(mp) should be (PolygonResult(p))
    }

    it ("should symDifference with a MultiPoint and return a GeometryCollectionResult") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val mp = MultiPoint(Seq(Point(50,50), Point(60,60)))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(50,50), Point(60,60)), polygons = Seq(p))
      val result = p.symDifference(mp)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
       case _ => fail()
      }
    }

    it ("should symDifference with a MultiLine and return a PolygonResult") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val ml = MultiLine(Seq(Line(Point(5,5), Point(6,6))))
      p.symDifference(ml) should be (PolygonResult(p))
    }

    it ("should symDifference with a MultiLine and return a GeometryCollectionResult") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val ml = MultiLine(Seq(Line(Point(50,50), Point(60,60))))
      val expected: GeometryCollection =
        GeometryCollection(lines = Seq(Line(Point(50,50), Point(60,60))), polygons = Seq(p))
      val result = p.symDifference(ml)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a MultiPolygon and return a NoResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(10,0), Point(10,10), Point(0,10), Point(0,0), Point(10,0)))
      val mp = MultiPolygon(Seq(p2))
      p1.symDifference(mp) should be (NoResult)
    }

    it ("should symDifference with a Polygon and return a NoResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(10,0), Point(10,10), Point(0,10), Point(0,0), Point(10,0)))
      val mp = MultiPolygon(Seq(p2))
      p1.symDifference(mp) should be (NoResult)
    }

    it ("should symDifference with a Polygon and return a PolygonResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      p1.symDifference(p2) should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,10), Point(5,10),
                                                                 Point(5,0), Point(0,0)))))
    }

    it ("should symDifference with a Polygon and return a MultiPolygonResult") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(15,0), Point(15,10), Point(25,10), Point(25,0), Point(15,0)))
      p1.symDifference(p2) should be (MultiPolygonResult(Seq(p1, p2)))
    }

    // -- Buffer

    it ("should have a buffer which is a Polygon") {
      val p: Polygon = SineStar().withSize(100).build()
      val result = p.buffer(5)
      result match {
        case Polygon(_) => // expected
        case _ => fail()
      }
    }

    // -- Predicates

    it ("should contain a Point") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt = Point(1,1)
      p.contains(pt) should be (true)
    }

    it ("should contain a MultiPoint") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt1 = Point(1,1)
      val pt2 = Point(0,0)
      val mp = MultiPoint(pt1, pt2)
      p.contains(mp) should be (true)
    }

    it ("should contain a Line") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l = Line(Point(1,1), Point(9,9))
      p.contains(l) should be (true)
    }

    it ("should contain a MultiLine") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l1 = Line(Point(1,1), Point(9,9))
      val l2 = Line(Point(5,5), Point(5,8))
      val ml = MultiLine(l1, l2)
      p.contains(ml) should be (true)
    }

    it ("should contain a Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      p1.contains(p2) should be (true)
    }

    it ("should contain a MultiPolygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      val p3 = Polygon(Line(Point(1,7), Point(1,8), Point(4,7), Point(1,7)))
      val mp = MultiPolygon(p2, p3)
      p1.contains(mp) should be (true)
    }

    it ("should be covered by another Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      p2.coveredBy(p1) should be (true)
    }

    it ("should cover a Point") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt = Point(1,1)
      p.covers(pt) should be (true)
    }

    it ("should cover a MultiPoint") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt1 = Point(1,1)
      val pt2 = Point(0,0)
      val mp = MultiPoint(pt1, pt2)
      p.covers(mp) should be (true)
    }

    it ("should cover a Line") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l = Line(Point(1,1), Point(9,9))
      p.covers(l) should be (true)
    }

    it ("should cover a MultiLine") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l1 = Line(Point(1,1), Point(9,9))
      val l2 = Line(Point(5,5), Point(5,8))
      val ml = MultiLine(l1, l2)
      p.covers(ml) should be (true)
    }

    it ("should cover a Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      p1.covers(p2) should be (true)
    }

    it ("should cover a MultiPolygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,0), Point(5,10), Point(10,10), Point(10,0), Point(5,0)))
      val p3 = Polygon(Line(Point(1,7), Point(1,8), Point(4,7), Point(1,7)))
      val mp = MultiPolygon(p2, p3)
      p1.covers(mp) should be (true)
    }

    it ("should cross with a Line") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l = Line(Point(-5,5), Point(15, 5))
      p.crosses(l) should be (true)
    }

    it ("should cross with a MultiLine") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l1 = Line(Point(3,3), Point(6,6))
      val l2 = Line(Point(0, 15), Point(15, 15))
      val ml = MultiLine(l1, l2)
      p.crosses(ml) should be (true)
    }

    it ("should cross with a MultiPoint") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p1 = Point(3,3)
      val p2 = Point(0, 15)
      val mp = MultiPoint(p1, p2)
      p.crosses(mp) should be (true)
    }

    it ("should overlap a Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(5,5), Point(5,15), Point(15,15), Point(15,5), Point(5,5)))
      p1.overlaps(p2) should be (true)
    }

    it ("should touch a Point") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt = Point(0,0)
      p.touches(pt) should be (true)
    }

    it ("should touch a MultiPoint") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val pt1 = Point(15,15)
      val pt2 = Point(0,0)
      val mp = MultiPoint(pt1, pt2)
      p.touches(mp) should be (true)
    }

    it ("should touch a Line") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l = Line(Point(10,10), Point(20,20))
      p.touches(l) should be (true)
    }

    it ("should touch a MultiLine") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val l1 = Line(Point(10,10), Point(19,19))
      val l2 = Line(Point(15,15), Point(15,18))
      val ml = MultiLine(l1, l2)
      p.touches(ml) should be (true)
    }

    it ("should touch a Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(0,0), Point(0,10), Point(-10,10), Point(-10,0), Point(0,0)))
      p1.touches(p2) should be (true)
    }

    it ("should touch a MultiPolygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(0,0), Point(0,10), Point(-10,10), Point(-10,0), Point(0,0)))
      val p3 = Polygon(Line(Point(11,17), Point(11,18), Point(14,17), Point(11,17)))
      val mp = MultiPolygon(p2, p3)
      p1.touches(mp) should be (true)
    }

    it ("should be within another Polygon") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(1,1), Point(1,9), Point(9,9), Point(9,1), Point(1,1)))
      p2.within(p1) should be (true)
    }

    it ("should maintain immutability over normalization") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      val expected = p.jtsGeom.clone
      p.normalized
      p.jtsGeom.equals(expected) should be (true)
    }

    it ("should maintain immutability over exterior") {
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)),
                      Line(Point(11,17), Point(11,18), Point(14,17), Point(11,17)))

      val expected = p.jtsGeom.clone
      val coord = p.exterior.points(0).jtsGeom.getCoordinate()
      val newCoord = Point(5,5).jtsGeom.getCoordinate()
      coord.setCoordinate(newCoord)

      p.jtsGeom.equals(expected) should be (true)
    }

    it("should let an invalid polygon remain invalid") {
      val l = Line( (0.0, 0.0), (1.0, 1.0), (0.0, 1.0), (1.0, 0.0), (0.0, 0.0))
      val p = Polygon(l)
      p.exterior should matchGeom(l)
    }
  }
}
