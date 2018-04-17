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

import org.locationtech.jts.{geom=>jts}

import org.scalatest._

class MultiPointSpec extends FunSpec with Matchers {
  describe("MultiPoint") {

    // -- Intersection

    it ("should intersect with a MultiPoint and return a NoResult") {
      val mp1 = MultiPoint(Point(1,1), Point(2,2))
      val mp2 = MultiPoint(Point(0,5), Point(5,5))
      mp1 & mp2 should be (NoResult)
    }

    it ("should intersect with a MultiPoint and return a PointResult") {
      val mp1 = MultiPoint(Point(1,1))
      val mp2 = MultiPoint(Point(1,1), Point(2,2))
      mp1 & mp2 should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Point(1,1), Point(2,2))
      val mp2 = MultiPoint(Point(1,1), Point(2,2), Point(3,3))
      mp1 & mp2 should be (MultiPointResult(Seq(Point(1,1), Point(2,2))))
    }

    it ("should intersect with a MultiLine and return a NoResult") {
      val mp = MultiPoint(Point(1,1), Point(2,2))
      val l1 = Line(Point(0,5), Point(5,5))
      val l2 = Line(Point(0,10), Point(5,10))
      val ml = MultiLine(l1, l2)
      mp & ml should be (NoResult)
    }

    it ("should intersect with a MultiLine and return a PointResult") {
      val mp = MultiPoint(Point(1,1), Point(2,2))
      val l1 = Line(Point(1,1), Point(1,5))
      val l2 = Line(Point(0,10), Point(5,10))
      val ml = MultiLine(l1, l2)
      mp & ml should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a MultiLine and return a MultiPointResult") {
      val mp = MultiPoint(Point(1,1), Point(2,2))
      val l1 = Line(Point(1,1), Point(5,5))
      val l2 = Line(Point(0,10), Point(5,10))
      val ml = MultiLine(l1, l2)
      mp & ml should be (MultiPointResult(Seq(Point(1,1), Point(2,2))))
    }

    it ("should intersect with a MultiPolygon and return a NoResult") {
      val mpt = MultiPoint(Point(1,1), Point(2,2))
      val p1 = Polygon(Line(Point(0,5), Point(5,5), Point(3,6), Point(0,5)))
      val p2 = Polygon(Line(Point(0,10), Point(5,10), Point(3,11), Point(0,10)))
      val mp = MultiPolygon(p1, p2)
      mpt & mp should be (NoResult)
    }

    it ("should intersect with a MultiPolygon and return a PointResult") {
      val mpt = MultiPoint(Point(1,1), Point(2,2))
      val p1 = Polygon(Line(Point(1,1), Point(5,6), Point(3,6), Point(1,1)))
      val p2 = Polygon(Line(Point(0,10), Point(5,10), Point(3,11), Point(0,10)))
      val mp = MultiPolygon(p1, p2)
      mpt & mp should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a MultiPolygon and return a MultiPointResult") {
      val mpt = MultiPoint(Point(1,1), Point(2,2))
      val p1 = Polygon(Line(Point(1,1), Point(5,5), Point(3,6), Point(1,1)))
      val p2 = Polygon(Line(Point(0,10), Point(5,10), Point(3,11), Point(0,10)))
      val mp = MultiPolygon(p1, p2)
      mpt & mp should be (MultiPointResult(Seq(Point(1,1), Point(2,2))))
    }

    // -- Union

    it("should union itself and merge points") {
      val mp =
        MultiPoint(
          (0.0, 0.0),
          (1.0, 1.0),
          (1.0, 1.0),
          (2.0, 2.0)
        )

      val expected =
        MultiPoint(
          (0.0, 0.0),
          (1.0, 1.0),
          (2.0, 2.0)
        )

      val actual =
        mp.union match {
          case MultiPointResult(mp) => mp
          case PointResult(p) => MultiPoint(p)
          case NoResult => MultiPoint()
        }

      actual should be (expected)
    }

    it ("should union with a MultiPoint and return a NoResult") {
      val mp1 = MultiPoint.EMPTY
      val mp2 = MultiPoint.EMPTY
      mp1 | mp2 should be (NoResult)
    }

    it ("should union with an empty MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Point(1,1))
      val mp2 = MultiPoint.EMPTY
      mp1 | mp2 should be (MultiPointResult(Seq(Point(1,1))))
    }

    it ("should union with a MultiPoint and return a PointResult") {
      val mp1 = MultiPoint(Point(1,1))
      val mp2 = MultiPoint(Seq(Point(1,1)))
      mp1 | mp2 should be (PointResult(Point(1,1)))
    }

    it ("should union with a MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Seq(Point(1,1)))
      val mp2 = MultiPoint(Seq(Point(5,5)))
      mp1 | mp2 should be (MultiPointResult(Seq(Point(1,1), Point(5,5))))
    }

    it ("should union with a MultiLine and return a NoResult") {
      val mp = MultiPoint.EMPTY
      val ml = MultiLine(Seq())
      mp | ml should be (NoResult)
    }

    it ("should union with a MultiLine and return a LineResult") {
      val mp = MultiPoint(Seq(Point(1,1)))
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      mp | ml should be (LineResult(Line(Point(1,1), Point(5,5))))
    }

    it ("should union with an empty MultiLine and return a MultiPointResult") {
      val mp = MultiPoint(Seq(Point(1,1)))
      val ml = MultiLine(Seq())
      mp | ml should be (MultiPointResult(Seq(Point(1,1))))
    }

    it ("should union with a MultiLine and return a MultiPoint") {
      val mp = MultiPoint(Seq(Point(1,1), Point(4,4)))
      val ml = MultiLine(Seq())
      mp | ml should be (MultiPointResult(Seq(Point(1,1), Point(4,4))))
    }

    it ("should union with a MultiLine and return a GeometryCollectionResult") {
      val mp = MultiPoint(Seq(Point(1,1)))
      val ml = MultiLine(Seq(Line(Point(0,5), Point(5,5))))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(1,1)), lines = Seq(Line(Point(0,5), Point(5,5))))
      val result = mp | ml
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a MultiPolygon and return a NoResult") {
      val mpt = MultiPoint.EMPTY
      val mp = MultiPolygon(Seq())
      mpt | mp should be (NoResult)
    }

   it ("should union with an empty MultiPolygon and return a MultiPointResult") {
      val mpt = MultiPoint(Seq(Point(1,1)))
      val mp = MultiPolygon(Seq())
      mpt | mp should be (MultiPointResult(Seq(Point(1,1))))
    }

    it ("should union with a MultiPolygon and return a PolygonResult") {
      val mpt = MultiPoint(Seq(Point(1,1)))
      val p = Polygon(Line(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0)))
      val mp = MultiPolygon(p)
      mpt | mp should be (PolygonResult(p))
    }

    it ("should union with a MultiPolygon and return a MultiPolygonResult") {
      val mpt = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      mpt | mp should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it ("should union with a MultiPolygon and return a GeometryCollectionResult") {
      val mpt = MultiPoint(Seq(Point(1,11), Point(5,15)))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(1,11), Point(5, 15)), polygons = Seq(p1, p2))
      val result = mpt | mp
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Difference

    it ("should difference with a Geometry and return a NoResult") {
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val l = Line(Point(1,1), Point(5,5))
      mp - l should be (NoResult)
    }

    it ("should difference with a Geometry and return a PointResult") {
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val p = Point(1,1)
      mp - p should be (PointResult(Point(5,5)))
    }

    it ("should difference with a Geometry and return a MultiPointResult") {
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val ml = MultiLine(Seq())
      mp - ml should be (MultiPointResult(Seq(Point(1,1), Point(5,5))))
    }

    // -- SymDifference

    it ("should symDifference with a MultiPoint and return a NoResult") {
      val mp1 = MultiPoint.EMPTY
      val mp2 = MultiPoint.EMPTY
      mp1.symDifference(mp2) should be (NoResult)
    }

    it ("should symDifference with a MultiPoint and return a PointResult") {
      val mp1 = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val mp2 = MultiPoint(Seq(Point(1,1)))
      mp1.symDifference(mp2) should be (PointResult(Point(5,5)))
    }

    it ("should symDifference with a MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val mp2 = MultiPoint(Seq(Point(1,1), Point(4,4)))
      mp1.symDifference(mp2) should be (MultiPointResult(Seq(Point(4,4), Point(5,5))))
    }

    it ("should symDifference with a MultiLine and return a NoResult") {
      val mp = MultiPoint.EMPTY
      val ml = MultiLine(Seq())
      mp.symDifference(ml) should be (NoResult)
    }

    // I thought this would be a PointResult
    it ("should symDifference with an empty MultiLine and return a MultiPointResult") {
      val mp = MultiPoint(Seq(Point(1,1)))
      val ml = MultiLine(Seq())
      mp.symDifference(ml) should be (MultiPointResult(Seq(Point(1,1))))
    }

    it ("should symDifference with a MultiLine and return a LineResult") {
      val mp = MultiPoint(Seq(Point(2,2)))
      val l = Line(Point(1,1), Point(5,5))
      val ml = MultiLine(Seq(l))
      mp.symDifference(ml) should be (LineResult(l))
    }

    it ("should symDifference with an empty MultiLine and return a MultiPointResult with more than one Point") {
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val ml = MultiLine(Seq())
      mp.symDifference(ml) should be (MultiPointResult(Seq(Point(1,1), Point(5,5))))
    }

    it ("should symDifference with a MultiLine and return a MultiLineResult") {
      val mp = MultiPoint(Seq(Point(2,2)))
      val l1 = Line(Point(1,1), Point(5,5))
      val l2 = Line(Point(2,6), Point(6,6))
      val ml = MultiLine(Seq(l1, l2))
      mp.symDifference(ml) should be (MultiLineResult(Seq(l1, l2)))
    }

    it ("should symDifference with a MultiLine and return a GeometryCollectionResult") {
      val mp = MultiPoint(Seq(Point(12,12)))
      val l1 = Line(Point(1,1), Point(5,5))
      val l2 = Line(Point(2,6), Point(6,6))
      val ml = MultiLine(Seq(l1, l2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(12,12)), lines = Seq(l1, l2))
      val result = mp.symDifference(ml)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should symDifference with a MultiPolygon and return a NoResult") {
      val mpt = MultiPoint.EMPTY
      val mp = MultiPolygon(Seq())
      mpt.symDifference(mp) should be (NoResult)
    }

    // I thought this would be a PointResult
    it ("should symDifference with an empty MultiPolygon and return a MultiPointResult") {
      val mpt = MultiPoint(Seq(Point(1,1)))
      val mp = MultiPolygon(Seq())
      mpt.symDifference(mp) should be (MultiPointResult(Seq(Point(1,1))))
    }

    it ("should symDifference with a MultiPolygon and return a PolygonResult") {
      val mpt = MultiPoint(Seq(Point(2,2)))
      val p = Polygon(Line(Point(1,1), Point(5,5), Point(1,5), Point(1,1)))
      val mp = MultiPolygon(Seq(p))
      mpt.symDifference(mp) should be (PolygonResult(p))
    }

    it ("should symDifference with an empty MultiPolygon and return a MultiPointResult with more than one Point") {
      val mpt = MultiPoint(Seq(Point(1,1), Point(5,5)))
      val mp = MultiPolygon(Seq())
      mpt.symDifference(mp) should be (MultiPointResult(Seq(Point(1,1), Point(5,5))))
    }

    it ("should symDifference with a MultiPolygon and return a MultiPolygonResult") {
      val mpt = MultiPoint(Seq(Point(2,2)))
      val p1 = Polygon(Line(Point(1,1), Point(5,5), Point(1,5), Point(1,1)))
      val p2 = Polygon(Line(Point(2,6), Point(6,6), Point(6, 10), Point(2,6)))
      val mp = MultiPolygon(Seq(p1, p2))
      mpt.symDifference(mp) should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it ("should symDifference with a MultiPolygon and return a GeometryCollectionResult") {
      val mpt = MultiPoint(Seq(Point(0,0)))
      val p1 = Polygon(Line(Point(1,1), Point(1,5), Point(5,5), Point(5,1), Point(1,1)))
      val p2 = Polygon(Line(Point(1,11), Point(1,15), Point(5, 15), Point(5,11), Point(1,11)))
      val mp = MultiPolygon(Seq(p1, p2))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(0,0)), polygons = Seq(p1, p2))
      val result = mpt.symDifference(mp)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Convex Hull

    it ("should convexHull and return a Polygon") {
      // if all 3 of these points form a straight line, then it doesn't return a Polygon
      val mp = MultiPoint(Point(1,1), Point(2,2), Point(3,13))
      val result = mp.convexHull()
      result match {
        case PolygonResult(_) => // expected
        case _ => fail()
      }
    }

    // -- Predicates

    it ("should contain a Point") {
      val mp = MultiPoint(Point(1,1), Point(5,5))
      val p = Point(1,1)
      mp.contains(p) should be (true)
    }

    it ("should contain a MultiPoint") {
      val mp1 = MultiPoint(Point(1,1), Point(5,5), Point(6,6))
      val mp2 = MultiPoint(Point(1,1), Point(6,6))
      mp1.contains(mp2) should be (true)
    }

    it ("should be covered by a Point") {
      val mp = MultiPoint(Point(1,1))
      val p = Point(1,1)
      mp.coveredBy(p) should be (true)
    }

    it ("should be cover by a MultiPoint") {
      val mp1 = MultiPoint(Point(1,1), Point(2,2), Point(3,3))
      val mp2 = MultiPoint(Point(1,1), Point(3,3))
      mp1.covers(mp2) should be (true)
    }

    it ("should cross a Line") {
      val mp = MultiPoint(Seq(Point(5,5), Point(0,0)))
      val l = Line(Point(3,3), Point(6,6))
      mp.crosses(l) should be (true)
    }

    it ("should cross a MultiPolygon") {
      val mp = MultiPoint(Seq(Point(5,5), Point(0,0)))
      val mpoly =
        MultiPolygon(Seq(Polygon(Line(Point(3,3), Point(3,6), Point(6,6), Point(6,3), Point(3,3)))))
      mp.crosses(mpoly) should be (true)
    }

    it ("should overlap a MultiPoint") {
      val mp1 = MultiPoint(Point(1,1), Point(2,2))
      val mp2 = MultiPoint(Point(1,1), Point(3,3))
      mp1.overlaps(mp2) should be (true)
    }

    it ("should touch a Line") {
      val mp = MultiPoint(Point(1,1), Point(2,2))
      val l = Line(Point(2,2), Point(5,5))
      mp.touches(l) should be (true)
    }

    it ("should touch a Polygon") {
      val mp = MultiPoint(Point(1,1), Point(2,2))
      val p = Polygon(Line(Point(2,2), Point(2,5), Point(5,5), Point(5,2), Point(2,2)))
      mp.touches(p) should be (true)
    }

    it ("should be within a Point") {
      val mp = MultiPoint(Point(1,1))
      val p = Point(1,1)
      mp.within(p) should be (true)
    }

    it ("should maintain immutability over normalization") {
      val mp = MultiPoint(Point(2,2), Point(1,1), Point(3,2), Point(1,1))
      val expected = mp.jtsGeom.clone
      mp.normalized
      mp.jtsGeom.equals(expected) should be (true)
    }

    it ("should maintain immutability over points") {
      val mp = MultiPoint(Point(1,1), Point(2,2))

      val expected = mp.jtsGeom.clone

      val coord = mp.points(0).jtsGeom.getCoordinate()
      val newCoord = Point(5,5).jtsGeom.getCoordinate()
      coord.setCoordinate(newCoord)

      mp.jtsGeom.equals(expected) should be (true)
    }

  }

}
