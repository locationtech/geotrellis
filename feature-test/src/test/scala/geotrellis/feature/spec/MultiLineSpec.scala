/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class MultiLineSpec extends FunSpec with ShouldMatchers {

  describe("MultiLine") {

    // -- Misc.

    it("should be able to be decomposed into its constituent lines") {
      val l1 = Line(Point(1,1), Point(5,5))
      val l2 = Line(Point(1,5), Point(1,10))
      val ml = MultiLine(l1, l2)
      ml.lines should be (Array(l1, l2))
    }

    it("should be closed if all the individual lines are closed") {
      val l1 = Line(Point(1,1), Point(2,2), Point(1,1))
      val l2 = Line(Point(5,5), Point(6, 10), Point(5,5))
      val ml = MultiLine(l1, l2)
      ml.isClosed should be (true)
    }

    // -- Boundary

    it("should have an empty boundary if it is closed") {
      val l1 = Line(Point(1,1), Point(2,2), Point(1,1))
      val l2 = Line(Point(5,5), Point(6, 10), Point(5,5))
      val ml = MultiLine(l1, l2)
      ml.boundary should be (NoResult)
    }

    it("should have a non-empty boundary if it is not closed") {
      val l1 = Line(Point(1,1), Point(2,2))
      val l2 = Line(Point(5,5), Point(6, 10))
      val l3 = Line(Point(10,10), Point(12,12), Point(10,10))
      val ml = MultiLine(l1, l2, l3)
      ml.boundary should be (MultiPointResult(Seq(Point(1,1), Point(2,2), Point(5,5), Point(6,10))))
    }

    // -- Intersection

    it("should intersect with a MultiPolygon and return a NoResult") {
      val l1 = Line(Point(1,1), Point(1,10))
      val l2 = Line(Point(2,1), Point(2, 10))
      val p1 = Polygon(Line(Point(3,0), Point(6,0), Point(6,3), Point(3,3), Point(3,0)))
      val p2 = Polygon(Line(Point(3,5), Point(6,5), Point(6,8), Point(3,8), Point(3,5)))
      val ml = MultiLine(l1,l2)
      val mp = MultiPolygon(p1, p2)
      ml & mp should be (NoResult)
    }

    it("should intersect with a MultiPolygon and return a PointResult") {
      val l1 = Line(Point(1,1), Point(1,10))
      val l2 = Line(Point(3,8), Point(2, 10))
      val p1 = Polygon(Line(Point(3,0), Point(6,0), Point(6,3), Point(3,3), Point(3,0)))
      val p2 = Polygon(Line(Point(3,5), Point(6,5), Point(6,8), Point(3,8), Point(3,5)))
      val ml = MultiLine(l1,l2)
      val mp = MultiPolygon(p1, p2)
      ml & mp should be (PointResult(Point(3,8)))
    }

    it("should intersect with a MultiPolygon and return a LineResult") {
      val l1 = Line(Point(1,0), Point(10,0))
      val l2 = Line(Point(2,1), Point(2, 10))
      val p1 = Polygon(Line(Point(3,0), Point(6,0), Point(6,3), Point(3,3), Point(3,0)))
      val p2 = Polygon(Line(Point(3,5), Point(6,5), Point(6,8), Point(3,8), Point(3,5)))
      val ml = MultiLine(l1,l2)
      val mp = MultiPolygon(p1, p2)
      ml & mp should be (LineResult(Line(Point(3,0), Point(6,0))))
    }

    it("should intersect with a MultiPolygon and return a MultiPointResult") {
      val l1 = Line(Point(3,0), Point(0,3), Point(3,5))
      val l2 = Line(Point(2,1), Point(2, 10))
      val p1 = Polygon(Line(Point(3,0), Point(6,0), Point(6,3), Point(3,3), Point(3,0)))
      val p2 = Polygon(Line(Point(3,5), Point(6,5), Point(6,8), Point(3,8), Point(3,5)))
      val ml = MultiLine(l1,l2)
      val mp = MultiPolygon(p1, p2)
      ml & mp should be (MultiPointResult(Seq(Point(3,0), Point(3,5))))
    }

    it("should intersect with a MultiPolygon and return a MultiLineResult") {
      val l1 = Line(Point(1,0), Point(10,0))
      val l2 = Line(Point(1,8), Point(10, 8))
      val p1 = Polygon(Line(Point(3,0), Point(6,0), Point(6,3), Point(3,3), Point(3,0)))
      val p2 = Polygon(Line(Point(3,5), Point(6,5), Point(6,8), Point(3,8), Point(3,5)))
      val ml = MultiLine(l1,l2)
      val mp = MultiPolygon(p1, p2)
      ml & mp should be (MultiLineResult(Seq(Line(Point(3,0), Point(6,0)), Line(Point(3,8), Point(6,8)))))
    }

    it("should intersect with a MultiPolygon and return a GeometryCollectionResult") {
      val l1 = Line(Point(1, 0), Point(10, 0))
      val l2 = Line(Point(6, 8), Point(6, 18))
      val p1 = Polygon(Line(Point(3, 0), Point(6, 0), Point(6, 3), Point(3, 3), Point(3, 0)))
      val p2 = Polygon(Line(Point(3, 5), Point(6, 5), Point(6, 8), Point(3, 8), Point(3, 5)))
      val ml = MultiLine(l1, l2)
      val mp = MultiPolygon(p1, p2)
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(6, 8)), lines = Seq(Line(Point(3, 0), Point(6, 0))))
      val result = ml & mp
      result match {
        case GeometryCollectionResult(gc) => gc should be(expected)
        case _ => fail()
      }
    }

    // -- Union

    it("should union with a MultiLine and return a NoResult") {
      val ml1 = MultiLine(Seq())
      val ml2 = MultiLine(Seq())
      ml1 | ml2 should be (NoResult)
    }

    it("should union with a MultiLine and return a LineResult") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val ml2 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      ml1 | ml2 should be (LineResult(Line(Point(1,1), Point(5,5))))
    }

    it("should union with a MultiLine and return a MultiLineResult") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val ml2 = MultiLine(Seq(Line(Point(1,1), Point(1,5))))
      ml1 | ml2 should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(1,1), Point(1,5)))))
    }

    it("should union with a MultiPolygon and return a NoResult") {
      val ml = MultiLine(Seq())
      val mp = MultiPolygon(Seq())
      ml | mp should be (NoResult)
    }

    it("should union with an empty MultiPolygon and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val mp = MultiPolygon(Seq())
      ml | mp should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)))))
    }

    it("should union with a MultiPolygon and return a PolygonResult") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(2,2))))
      val p = Polygon(Line(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0)))
      val mp = MultiPolygon(p)
      ml | mp should be (PolygonResult(p))
    }

    it("should union with a MultiPolygon and return a MultiPolygonResult") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(2,2))))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      ml | mp should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it("should union with a MultiPolygon and return a GeometryCollectionResult") {
      val ml = MultiLine(Seq(Line(Point(1,11), Point(1,21))))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      val expected: GeometryCollection =
        GeometryCollection(lines = Seq(Line(Point(1,11), Point(1,21))), polygons = Seq(p1, p2))
      val result = ml | mp
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Difference

    it("should difference with a Point and return a NoResult") {
      val ml = MultiLine(Seq())
      val p = Point(1,1)
      ml - p should be (NoResult)
    }

    it("should difference with a MultiPoint and return a NoResult") {
      val ml = MultiLine(Seq())
      val mp = MultiPoint(Seq(Point(1,1), Point(2,2)))
      ml - mp should be (NoResult)
    }

    it("should difference with a Line and return a NoResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val l = Line(Point(1,1), Point(5,5))
      ml - l should be (NoResult)
    }

    it("should difference with a MultiLine and return a NoResult") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val ml2 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      ml1 - ml2 should be (NoResult)
    }

    it("should difference with a Polygon and return a NoResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))
      ml - p should be (NoResult)
    }

    it("should difference with a MultiPolygon and return a NoResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
      ml - mp should be (NoResult)
    }

    it("should difference with a Point and return a LineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val p = Point(1,1)
      ml - p should be (LineResult(Line(Point(1,1), Point(5,5))))
    }

    it("should difference with a MultiPoint and return a LineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      ml - mp should be (LineResult(Line(Point(1,1), Point(5,5))))
    }

    it("should difference with a Line and return a LineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10))))
      val l = Line(Point(1,1), Point(5,5))
      ml - l should be (LineResult(Line(Point(0,0), Point(0,10))))
    }

    it("should difference with a MultiLine and return a LineResult") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10))))
      val ml2 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      ml1 - ml2 should be (LineResult(Line(Point(0,0), Point(0,10))))
    }

    it("should difference with a Polygon and return a LineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(20,20), Point(20,30))))
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))
      ml - p should be (LineResult(Line(Point(20,20), Point(20,30))))
    }

    it("should difference with a MultiPolygon and return a LineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(20,20), Point(20,30))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
      ml - mp should be (LineResult(Line(Point(20,20), Point(20,30))))
    }

    it("should difference with a Point and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0, 5))))
      val p = Point(1,1)
      ml - p should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0, 5)))))
    }

    it("should difference with a MultiPoint and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0, 5))))
      val mp = MultiPoint(Seq(Point(1,1), Point(5,5)))
      ml - mp should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0, 5)))))
    }

    it("should difference with a Line and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10))))
      val l = Line(Point(10,10), Point(50,50))
      ml - l should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10)))))
    }

    it("should difference with a MultiLine and return a MultiLineResult") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10))))
      val ml2 = MultiLine(Seq())
      ml1 - ml2 should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10)))))
    }

    it("should difference with a Polygon and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(2,2), Point(5,5)), Line(Point(20,20), Point(20,30))))
      val p = Polygon(Line(Point(0,0), Point(0,1), Point(1,1), Point(1, 0), Point(0,0)))
      ml - p should be (MultiLineResult(Seq(Line(Point(2,2), Point(5,5)), Line(Point(20,20), Point(20,30)))))
    }

    it("should difference with a MultiPolygon and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(20,20), Point(20,30))))
      val mp = MultiPolygon(Seq())
      ml - mp should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(20,20), Point(20,30)))))
    }


    // -- SymDifference

    it("should symDifference with a MultiLine and return a NoResult") {
      val ml1 = MultiLine(Seq())
      val ml2 = MultiLine(Seq())
      ml1.symDifference(ml2) should be (NoResult)
    }

    it("should symDifference with a MultiLine and return a LineResult") {
      val ml1 = MultiLine(Seq(Line(Point(3,3), Point(5,5))))
      val ml2 = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      ml1.symDifference(ml2) should be (LineResult(Line(Point(1,1), Point(3,3))))
    }

    it("should symdifference with a MultiPolygon and return a NoResult") {
      val ml = MultiLine(Seq())
      val mp = MultiPolygon(Seq())
      ml.symDifference(mp) should be (NoResult)
    }

    // I thought this would be a LineResult
    it("should symDifference with an empty MultiPolygon and return a MultiLineResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val mp = MultiPolygon(Seq())
      ml.symDifference(mp) should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)))))
    }

    it("should symDifference with an empty MultiPolygon and return a MultiLineResult " +
        "when there are multiple lines in the Seq") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10))))
      val mp = MultiPolygon(Seq())
      ml.symDifference(mp) should be (MultiLineResult(Seq(Line(Point(1,1), Point(5,5)), Line(Point(0,0), Point(0,10)))))
    }

    it("should symDifference with a MultiPolygon and return a PolygonResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
      ml.symDifference(mp) should be (PolygonResult(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
    }

    it("should symDifference with a MultiPolygon and return a MultiPolygonResult") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(5,5))))
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))
      val p2 = Polygon(Line(Point(0,20), Point(0,30), Point(10,30), Point(10, 20), Point(0,20)))
      val mp = MultiPolygon(Seq(p1, p2))
      ml.symDifference(mp) should be (MultiPolygonResult(Seq(p1, p2)))
    }

    it("should symDifference with a MultiPolygon and return a GeometryCollectionResult") {
      val ml = MultiLine(Seq(Line(Point(1,11), Point(5,51))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
      val expected: GeometryCollection =
        GeometryCollection(lines = Seq(Line(Point(1,11), Point(5,51))),
            polygons = Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10, 0), Point(0,0)))))
      val result = ml.symDifference(mp)
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Predicates

    it("should contain a Point") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val p = Point(5,0)
      ml.contains(p) should be (true)
    }

    it("should contain a MultiPoint") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val mp = MultiPoint(Seq(Point(5,0), Point(0,8)))
      ml.contains(mp) should be (true)
    }

    it("should contain a Line") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val l = Line(Point(4,0), Point(6,0))
      ml.contains(l) should be (true)
    }

    it("should contain a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val ml2 = MultiLine(Seq(Line(Point(4,0), Point(6,0)), Line(Point(0,0), Point(0,5))))
      ml1.contains(ml2) should be (true)
    }

    it("should be covered by a line") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(3,3)), Line(Point(5,5), Point(7,7))))
      val l = Line(Point(0,0), Point(10,10))
      ml.coveredBy(l) should be (true)
    }

    it("should be covered by a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(1,1), Point(3,3)), Line(Point(5,5), Point(7,7))))
      val ml2 = MultiLine(Seq(Line(Point(0,0), Point(10,10))))
      ml1.coveredBy(ml2) should be (true)
    }

    it("should be covered by a Polygon") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(3,3)), Line(Point(5,5), Point(7,7))))
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      ml.coveredBy(p) should be (true)
    }

    it("should be covered by a MultiPolygon") {
      val ml = MultiLine(Seq(Line(Point(1,1), Point(3,3)), Line(Point(5,5), Point(7,7))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))))
      ml.coveredBy(mp) should be (true)
    }

    it("should cover a Point") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val p = Point(5,0)
      ml.contains(p) should be (true)
    }

    it("should cover a MultiPoint") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val mp = MultiPoint(Seq(Point(5,0), Point(0,8)))
      ml.contains(mp) should be (true)
    }

    it("should cover a Line") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val l = Line(Point(4,0), Point(6,0))
      ml.contains(l) should be (true)
    }

    it("should cover a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val ml2 = MultiLine(Seq(Line(Point(4,0), Point(6,0)), Line(Point(0,0), Point(0,5))))
      ml1.contains(ml2) should be (true)
    }

    it("should cross a MultiPoint") {
      val mp = MultiPoint(Seq(Point(5,5), Point(0,0)))
      val ml = MultiLine(Seq(Line(Point(3,3), Point(6,6))))
      ml.crosses(mp) should be (true)
    }

    it("should cross a Line") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val l = Line(Point(5,5), Point(5,-5))
      ml.crosses(l) should be (true)
    }

    it("should cross a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val ml2 = MultiLine(Seq(Line(Point(5,5), Point(5,-5))))
      ml1.crosses(ml2) should be (true)
    }

    it("should cross a Polygon") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val p = Polygon(Line(Point(5,5), Point(5,-5), Point(7,-5), Point(7,7), Point(5,5)))
      ml.crosses(p) should be (true)
    }

    it("should cross a MultiPolygon") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(5,5), Point(5,-5), Point(7,-5), Point(7,7), Point(5,5)))))
      ml.crosses(mp) should be (true)
    }

    it("should overlap a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(0,0), Point(5,5))))
      val ml2 = MultiLine(Seq(Line(Point(3,3), Point(10,10))))
      ml1.overlaps(ml2) should be (true)
    }

    it("should touch a Point") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val p = Point(10,0)
      ml.touches(p) should be (true)
    }

    it("should touch a MultiPoint") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val mp = MultiPoint(Seq(Point(10,0), Point(0,10)))
      ml.touches(mp) should be (true)
    }

    it("should touch a Line") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val l = Line(Point(10,0), Point(10,5))
      ml.touches(l) should be (true)
    }

    it("should touch a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val ml2 = MultiLine(Seq(Line(Point(10,0), Point(10,5))))
      ml1.touches(ml2) should be (true)
    }

    it("should touch a Polygon") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val p = Polygon(Line(Point(10,0), Point(10,5), Point(12,5), Point(12,0), Point(10,0)))
      ml.touches(p) should be (true)
    }

    it("should touch a MultiPolygon") {
      val ml = MultiLine(Seq(Line(Point(0,0), Point(0,10)), Line(Point(0,0), Point(10,0))))
      val mp = MultiPolygon(Seq(Polygon(Line(Point(10,0), Point(10,5), Point(12,5), Point(12,0), Point(10,0)))))
      ml.touches(mp) should be (true)
    }

    it("should be within a Line") {
      val ml = MultiLine(Seq(Line(Point(3,3), Point(5,5)), Line(Point(6,6), Point(8,8))))
      val l = Line(Point(0,0), Point(10,10))
      ml.within(l) should be (true)
    }

    it("should be within a MultiLine") {
      val ml1 = MultiLine(Seq(Line(Point(3,3), Point(5,5)), Line(Point(6,6), Point(8,8))))
      val ml2 = MultiLine(Seq(Line(Point(0,0), Point(10,10))))
      ml1.within(ml2) should be (true)
    }

    it("should be within a Polygon") {
      val ml = MultiLine(Seq(Line(Point(3,3), Point(5,5)), Line(Point(6,6), Point(8,8))))
      val p = Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))
      ml.within(p) should be (true)
    }

    it("should be within a MultiPolygon") {
      val ml = MultiLine(Seq(Line(Point(3,3), Point(5,5)), Line(Point(6,6), Point(8,8))))
      val p = MultiPolygon(Seq(Polygon(Line(Point(0,0), Point(0,10), Point(10,10), Point(10,0), Point(0,0)))))
      ml.within(p) should be (true)
    }

  }

}
