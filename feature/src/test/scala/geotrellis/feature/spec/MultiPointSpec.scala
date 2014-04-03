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

class MultiPointSpec extends FunSpec with ShouldMatchers {
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
      mp1 & mp2 should be (MultiPointResult(Set(Point(1,1), Point(2,2))))
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
      mp & ml should be (MultiPointResult(Set(Point(1,1), Point(2,2))))
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
      mpt & mp should be (MultiPointResult(Set(Point(1,1), Point(2,2))))
    }

    // -- Union

    it ("should union with a MultiPoint and return a NoResult") {
      val mp1 = MultiPoint(Set())
      val mp2 = MultiPoint(Set())
      mp1 | mp2 should be (NoResult)
    }

    it ("should union with an empty MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Point(1,1))
      val mp2 = MultiPoint(Set())
      mp1 | mp2 should be (MultiPointResult(Set(Point(1,1))))
    }

    it ("should union with a MultiPoint and return a PointResult") {
      val mp1 = MultiPoint(Point(1,1))
      val mp2 = MultiPoint(Set(Point(1,1)))
      mp1 | mp2 should be (PointResult(Point(1,1)))
    }

    it ("should union with a MultiPoint and return a MultiPointResult") {
      val mp1 = MultiPoint(Set(Point(1,1)))
      val mp2 = MultiPoint(Set(Point(5,5)))
      mp1 | mp2 should be (MultiPointResult(Set(Point(1,1), Point(5,5))))
    }

    it ("should union with a MultiLine and return a NoResult") {
      val mp = MultiPoint(Set())
      val ml = MultiLine(Set())
      mp | ml should be (NoResult)
    }

    it ("should union with a MultiLine and return a LineResult") {
      val mp = MultiPoint(Set(Point(1,1)))
      val ml = MultiLine(Set(Line(Point(1,1), Point(5,5))))
      mp | ml should be (LineResult(Line(Point(1,1), Point(5,5))))
    }

    it ("should union with an empty MultiLine and return a MultiPointResult") {
      val mp = MultiPoint(Set(Point(1,1)))
      val ml = MultiLine(Set())
      mp | ml should be (MultiPointResult(Set(Point(1,1))))
    }

    it ("should union with a MultiLine and return a MultiPoint") {
      val mp = MultiPoint(Set(Point(1,1), Point(4,4)))
      val ml = MultiLine(Set())
      mp | ml should be (MultiPointResult(Set(Point(1,1), Point(4,4))))
    }

    it ("should union with a MultiLine and return a GeometryCollectionResult") {
      val mp = MultiPoint(Set(Point(1,1)))
      val ml = MultiLine(Set(Line(Point(0,5), Point(5,5))))
      val expected: GeometryCollection =
        GeometryCollection(points = Set(Point(1,1)), lines = Set(Line(Point(0,5), Point(5,5))))
      val result = mp | ml
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should union with a MultiPolygon and return a NoResult") {
      val mpt = MultiPoint(Set())
      val mp = MultiPolygon(Set())
      mpt | mp should be (NoResult)
    }

   it ("should union with an empty MultiPolygon and return a MultiPointResult") {
      val mpt = MultiPoint(Set(Point(1,1)))
      val mp = MultiPolygon(Set())
      mpt | mp should be (MultiPointResult(Set(Point(1,1))))
    }

    it ("should union with a MultiPolygon and return a PolygonResult") {
      val mpt = MultiPoint(Set(Point(1,1)))
      val p = Polygon(Line(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0)))
      val mp = MultiPolygon(p)
      mpt | mp should be (PolygonResult(p))
//      val result = mpt | mp
//      result match {
//        case PolygonResult(poly) => poly.equals(p) should be (true)
//        case _ => fail()
//      }
    }

    it ("should union with a MultiPolygon and return a MultiPolygonResult") {
      val mpt = MultiPoint(Set(Point(1,1), Point(5,5)))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      mpt | mp should be (MultiPolygonResult(Set(p1, p2)))
    }

    it ("should union with a MultiPolygon and return a GeometryCollectionResult") {
      val mpt = MultiPoint(Set(Point(1,11), Point(5,15)))
      val p1 = Polygon(Line(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val p2 = Polygon(Line(Point(4,4), Point(4,6), Point(6,6), Point(6,4), Point(4,4)))
      val mp = MultiPolygon(p1, p2)
      val expected: GeometryCollection =
        GeometryCollection(points = Set(Point(1,11), Point(5, 15)), polygons = Set(p1, p2))
      val result = mpt | mp
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }




  }
}
