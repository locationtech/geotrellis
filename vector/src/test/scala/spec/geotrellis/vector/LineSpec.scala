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

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class LineSpec extends AnyFunSpec with Matchers {
  describe("Line") {

    it ("should be a closed Line if constructed with l(0) == l(-1)") {
      val l = LineString(List[(Double,Double)]((0,0),(1,0),(1,1),(0,1),(0,0)))
      l.isClosed should be (true)
    }

    it ("should return true for crosses for MultiLine it crosses") {
      val l = LineString( (0.0, 0.0), (5.0, 5.0) )
      val ml =
        MultiLineString (
          LineString( (1.0, 0.0), (1.0, 5.0) ),
          LineString( (2.0, 0.0), (2.0, 5.0) ),
          LineString( (3.0, 0.0), (3.0, 5.0) ),
          LineString( (4.0, 0.0), (4.0, 5.0) )
        )

      l.crosses(ml) should be (true)
    }

    it ("should be a simple LineString if it does not self-intersect at points other than the endpoints") {
      val l = LineString(Point(0,0), Point(2,2), Point(2, 10))
      l.isSimple should be (true)
    }

    it ("should not be a simple LineString if it does self-intersect at points other than the endpoints") {
      val l = LineString(Point(0,0), Point(2,2), Point(2,0), Point(0,2))
      l.isSimple should be (false)
    }

    it ("should have vertices equal to the set of Points that make up the LineString") {
      val l = LineString(Point(0,0), Point(1,1), Point(2,2))
      l.points should be (Array(Point(0,0), Point(1,1), Point(2,2)))
    }

    it ("should have a Polygon envelope whose points are (minx, miny), (minx, maxy), (maxx, maxy), (max, miny), (minx, miny)") {
      val l = LineString(Point(0,0), Point(2,3))
      l.extent should be (Extent(0,0,2,3))
    }

    it ("should close a line") {
      val l = LineString(Point(0,0), Point(2,0), Point(2,2), Point(0,2))
      l.closed should be (LineString(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0)))
    }

    it ("should close a line if already closed") {
      val l = LineString(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0))
      l.closed should be (LineString(Point(0,0), Point(2,0), Point(2,2), Point(0,2), Point(0,0)))
    }


    // -- Intersection

    it ("should intersect with a Point and return a PointResult") {
      val p = Point(1,1)
      val l = LineString(Point(0,0), Point(2,2))
      l & p should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a Point and return a NoResult") {
      val p = Point(10, 10)
      val l = LineString(Point(0,0), Point(2,2))
      l & p should be (NoResult)
    }

    it ("should intersect with a LineString and return a PointResult") {
      val l1 = LineString(Point(0,0), Point(2,2))
      val l2 = LineString(Point(0,2), Point(2,0))
      l1 & l2 should be (PointResult(Point(1,1)))
    }

    it ("should intersect with a LineString and return a LineStringResult") {
      val l1 = LineString(Point(0,0), Point(2,2))
      val l2 = LineString(Point(1,1), Point(3,3))
      l1 & l2 should be (LineStringResult(LineString(Point(1,1), Point(2,2))))
    }

    it ("should intersect with a LineString and return a MultiPointResult") {
      val l1 = LineString(Point(0,1), Point(4,1))
      val l2 = LineString(Point(1,2), Point(1,0), Point(3,0), Point(3,2))
      l1 & l2 should be (MultiPointResult(Seq(Point(1,1), Point(3,1)).toMultiPoint))
    }

    it ("should intersect with a LineString and return a MultiLineStringResult") {
      val l1 = LineString(Point(0,1), Point(4,1))
      val l2 = LineString(Point(1,1), Point(0,1), Point(2,0), Point(4,1), Point(3,1))
      l1 & l2 should be (MultiLineStringResult(Seq(LineString(Point(0,1), Point(1,1)), LineString(Point(3,1), Point(4,1))).toMultiLineString))
    }

    it ("should intersect with a LineString and return a NoResult") {
      val l1 = LineString(Point(0,0), Point(0,4))
      val l2 = LineString(Point(4,0), Point(4,4))
      l1 & l2 should be (NoResult)
    }

    it ("should intersect with a LineString and return a GeometryCollectionResult") {
      val l1 = LineString(Point(0,1), Point(4,1))
      val l2 = LineString(Point(0,1), Point(2,0), Point(4,1), Point(3,1))
      val expected: GeometryCollection =
        GeometryCollection(points = Seq(Point(0,1)), lines = Seq(LineString(Point(3,1), Point(4,1))))
      val result = l1 & l2
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    it ("should intersect with a MultiPoint and return a PointResult") {
      val l = LineString(Point(0,0), Point(2,2))
      val p1 = Point(1,1)
      val p2 = Point(5,5)
      val mp = MultiPoint(Seq(p1, p2))
      l & mp should be (PointResult(p1))
    }

    it ("should intersect with a MultiPoint and return a MultiPointResult") {
      val l = LineString(Point(0,0), Point(6,6))
      val p1 = Point(1,1)
      val p2 = Point(5,5)
      val mp = MultiPoint(Seq(p1, p2))
      l & mp should be (MultiPointResult(Seq(p1, p2).toMultiPoint))
    }

    it ("should intersect with a MultiPoint and return a NoResult") {
      val l = LineString(Point(10,0), Point(12,2))
      val p1 = Point(1,1)
      val p2 = Point(5,5)
      val mp = MultiPoint(Seq(p1, p2))
      l & mp should be (NoResult)
    }

    // -- Union

    it ("should union with a LineString and return a LineStringResult") {
      val l1 = LineString(Point(0,0), Point(3,3))
      val l2 = LineString(Point(0,0), Point(3,3))
      l1 | l2 should be (LineStringResult(LineString(Point(0,0), Point(3,3))))
    }

    it ("should union with a LineString and return a MultiLineStringResult") {
      val l1 = LineString(Point(0,0), Point(3,3))
      val l2 = LineString(Point(0,0), Point(0,3))
      l1 | l2 should be (MultiLineStringResult(Seq(LineString(Point(0,0), Point(3,3)), LineString(Point(0,0), Point(0,3))).toMultiLineString))
    }

    it ("should union with a MultiLineString and return a LineStringResult") {
      val l = LineString(Point(0,0), Point(3,3));
      val ml = MultiLineString(LineString(Point(0,0), Point(3,3)), LineString(Point(0,0), Point(3,3)))
      l | ml should be (LineStringResult(l))
    }

    it ("should union with a Polygon and return a PolygonResult") {
      val l = LineString(Point(0,0), Point(2,2))
      val p = Polygon(LineString(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      l | p should be (PolygonResult(p))
    }

    it ("should union with a Polygon and return a GeometryCollectionResult") {
      val l = LineString(Point(0,10), Point(2,10))
      val p = Polygon(LineString(Point(0,0), Point(0,2), Point(2,2), Point(2,0), Point(0,0)))
      val expected: GeometryCollection =
        GeometryCollection(lines = Seq(l), polygons = Seq(p))
      val result = l | p
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // This throws a topology exception
   it ("should union with a MultiPolygon and return a PolygonResult topo") {
     val l = LineString(Point(0,0), Point(2,2))
     val p1 = Polygon(LineString(Point(0,0), Point(0,2.1), Point(2.1,2.1), Point(2.1,0), Point(0,0)))
//     val p2 = Polygon(LineString(Point(0,0), Point(0,1.2), Point(2.1,0), Point(0,0)))
     val p2 = Polygon(LineString(Point(-5,-5), Point(-5,0), Point(0,-1), Point(-5,-5)))
     val mp = MultiPolygon(p1, p2)
     mp | l should be (MultiPolygonResult(mp))

   }

    it ("should union with an empty MultiPolygon and return a LineStringResult") {
      val l = LineString(Point(1,1), Point(2,1))
      val mp = MultiPolygon(Seq())
      l | mp should be (LineStringResult(l))

    }

 it ("should union with a MultiPolygon and return a PolygonResult") {
      val l = LineString(Point(1,1), Point(2,1))
      val p1 = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      val mp = MultiPolygon(Seq(p1))
      l | mp should be (PolygonResult(p1))
     }

    it ("should union with a MultiPolygon and return a MultiPolygonResult") {
      val l = LineString(Point(1,1), Point(2,1))
      val p1 = Polygon(LineString(Point(3,4), Point(3,5), Point(5,5), Point(5,3), Point(3,4)))
      val p2 = Polygon(LineString(Point(0.5,0.5), Point(2.5,0.5), Point(2.5,2.5), Point(0.5,2.5), Point(0.5,0.5)))
      val mp = MultiPolygon(p1, p2)
      val MultiPolygonResult(result) = l | mp
      result.normalized should be (MultiPolygon(p1, p2).normalized)
    }

    it ("should union with a MultiPolygon and return a GeometryCollectionResult") {
      val l = LineString(Point(10,10), Point(20,10))
      val p1 = Polygon(LineString(Point(1,2), Point(1,3), Point(3,3), Point(3,2), Point(1,2)))
      val p2 = Polygon(LineString(Point(0,0), Point(0,-4), Point(-4,-4), Point(-4,0), Point(0,0)))
      val mp = MultiPolygon(p1, p2)
      val expected: GeometryCollection =
        GeometryCollection(lines = Seq(l), polygons = Seq(p1, p2))
      val result = l | mp
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }

    // -- Difference

    it ("should difference with a Point and return a LineStringResult") {
      val l = LineString(Point(0,0), Point(3,3))
      val p = Point(2,2)
      l - p should be (LineStringResult(l))
    }

    it ("should difference with a MultiPoint and return a LineStringResult") {
      val l = LineString(Point(0,0), Point(3,3))
      val mp = MultiPoint(Point(2,2), Point(3,3))
      l - mp should be (LineStringResult(l))
    }

    it ("should difference with a LineString and return a NoResult") {
      val l1 = LineString(Point(1,1), Point(3,3))
      val l2 = LineString(Point(0,0), Point(4,4))
      l1 - l2 should be (NoResult)
    }

    it ("should difference with a LineString and return a LineStringResult") {
      val l1 = LineString(Point(1,1), Point(3,3))
      val l2 = LineString(Point(2,2), Point(4,4))
      l1 - l2 should be (LineStringResult(LineString(Point(1,1), Point(2,2))))
    }

    it ("should difference with a LineString and return a MultiLineStringResult") {
      val l1 = LineString(Point(0,0), Point(4,4))
      val l2 = LineString(Point(2,2), Point(3,3))
      l1 - l2 should be (MultiLineStringResult(Seq(LineString(Point(0,0), Point(2,2)), LineString(Point(3,3), Point(4,4))).toMultiLineString))
    }

    it ("should difference with a Polygon and return a NoResult") {
      val l = LineString(Point(1,1), Point(3,3))
      val p = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l - p should be (NoResult)
    }

    it ("should difference with a Polygon and return a LineStringResult") {
      val l = LineString(Point(2,4), Point(10,4))
      val p = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l - p should be (LineStringResult(LineString(Point(4,4), Point(10,4))))
    }

    it ("should difference with a Polygon and return a MultiLineStringResult") {
      val l = LineString(Point(-2,4), Point(10,4))
      val p = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l - p should be (MultiLineStringResult(Seq(LineString(Point(-2,4), Point(0,4)), LineString(Point(4,4), Point(10,4))).toMultiLineString))
    }

    // -- SymDifference

    // it ("should symDifference with a LineString and return NoResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(3,3), Point(0,0))
    //   l1.symDifference(l2) should be (NoResult)
    // }

    // it ("should symDifference with a LineString and return LineStringResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(1,1), Point(3,3))
    //   l1.symDifference(l2) should be (LineStringResult(LineString(Point(0,0), Point(1,1))))
    // }

    // it ("should symDifference with a LineString and return MultiLineStringResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(0,10), Point(10,10))
    //   l1.symDifference(l2) should be (MultiLineStringResult(Seq(l1, l2).toMultiLineString))
    // }

    // it ("should symDifference with a MultiLineString and return NoResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(3,3), Point(0,0))
    //   val l3 = LineString(Point(1,1), Point(2,2))
    //   val ml = MultiLineString(l2, l3)
    //   l1.symDifference(ml) should be (NoResult)
    // }

    // it ("should symDifference with a MultiLineString and return LineStringResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(3,3), Point(0,0))
    //   val l3 = LineString(Point(0,10), Point(10,10))
    //   val ml = MultiLineString(l2, l3)
    //   l1.symDifference(ml) should be (LineStringResult(l3))
    // }

    // it ("should symDifference with a MultiLineString and return MultiLineStringResult") {
    //   val l1 = LineString(Point(0,0), Point(3,3))
    //   val l2 = LineString(Point(0,10), Point(10,10))
    //   val l3 = LineString(Point(-2,-4), Point(-12, -14))
    //   val ml = MultiLineString(l2, l3)
    //   l1.symDifference(ml) should be (MultiLineStringResult(Seq(l1, l2, l3).toMultiLineString))
    // }

    // it ("should symDifference with a Polygon and return a PolygonResult") {
    //   val l = LineString(Point(3,1), Point(4,1))
    //   val p = Polygon(LineString(Point(2,0), Point(2,2), Point(5,2), Point(5,0), Point(2,0)))
    //   l.symDifference(p) should be (PolygonResult(p))
    // }

    // it ("should symDifference with a Polygon and return a GeometryCollectionResult") {
    //   val l = LineString(Point(0,12), Point(6,12))
    //   val p = Polygon(LineString(Point(2,0), Point(2,2), Point(4,2), Point(4,0), Point(2,0)))
    //   val expected: GeometryCollection =
    //     GeometryCollection(lines = Seq(l), polygons = Seq(p))
    //   val result = l.symDifference(p)
    //   result match {
    //     case GeometryCollectionResult(gc) => gc should be (expected)
    //     case _ => fail()
    //   }
    // }

    // it ("should symDifference with a MultiPolygon and return a MultiPolygonResult") {
    //   val l = LineString(Point(3,1), Point(4,1))
    //   val p1 = Polygon(LineString(Point(2,0), Point(2,2), Point(5,2), Point(5,0), Point(2,0)))
    //   val p2 = Polygon(LineString(Point(10,10), Point(10,11), Point(11,11), Point(10,10)))
    //   val mp = MultiPolygon(p1, p2)
    //   l.symDifference(mp) should be (MultiPolygonResult(Seq(p1, p2)))
    // }

    // it ("should symDifference with an empty MultiPolygon and return a LineStringResult") {
    //   val l = LineString(Point(3,1), Point(4,1))
    //   val mp = MultiPolygon(Seq())
    //   l.symDifference(mp) should be (LineStringResult(l))
    // }

    // -- Buffer

    it ("should buffer and return a Polygon") {
      val l = LineString(Point(1,1), Point(2,2))
      val result = l.buffer(1)
      result match {
        case _: Polygon => // expected
        case _ => fail()
      }
    }

    // -- Predicates

    it ("should contain a Point") {
      val p = Point(1,1)
      val l = LineString(Point(0,0), Point(2,2))
      l.contains(p) should be (true)
    }

    it ("should contain a MultiPoint") {
      val p1 = Point(1,1)
      val p2 = Point(2,2)
      val mp = MultiPoint(p1, p2)
      val l = LineString(Point(0,0), Point(2,2))
      l.contains(mp) should be (true)
    }

    it ("should contain a LineString") {
      val l1 = LineString(Point(0,0), Point(5,5))
      val l2 = LineString(Point(1,1), Point(3,3))
      l1.contains(l2) should be (true)
    }

    it ("should contain a MultiLineString") {
      val l1 = LineString(Point(0,0), Point(5,5))
      val l2 = LineString(Point(1,1), Point(3,3))
      val l3 = LineString(Point(2,2), Point(5,5))
      val mp = MultiLineString(l2, l3)
      l1.contains(mp) should be (true)
    }

    it ("should be covered by a LineString") {
      val l1 = LineString(Point(1,1), Point(2,2))
      val l2 = LineString(Point(0,0), Point(3,3))
      l1.coveredBy(l2) should be (true)
    }

    it ("should be covered by a Polygon") {
      val l = LineString(Point(1,1), Point(2,2))
      val p = Polygon(LineString(Point(0,0), Point(0,3), Point(3,3), Point(3,0), Point(0,0)))
      l.coveredBy(p) should be (true)
    }

    it ("should cover a Point") {
      val p = Point(2,2)
      val l = LineString(Point(0,0), Point(3,3))
      l.covers(p) should be (true)
    }

    it ("should cover a LineString") {
      val l1 = LineString(Point(0,0), Point(5,5))
      val l2 = LineString(Point(1,1), Point(3,3))
      l1.covers(l2) should be (true)
    }

    it ("should cover a MultiLineString") {
      val l1 = LineString(Point(0,0), Point(5,5))
      val l2 = LineString(Point(1,1), Point(3,3))
      val l3 = LineString(Point(2,2), Point(5,5))
      val mp = MultiLineString(l2, l3)
      l1.covers(mp) should be (true)
    }

    it ("should cross a LineString") {
      val l1 = LineString(Point(0,0), Point(5,5))
      val l2 = LineString(Point(0,5), Point(5,0))
      l1.crosses(l2) should be (true)
    }

    it ("should cross a Polygon") {
      val l = LineString(Point(0,0), Point(10,10))
      val p = Polygon(LineString(Point(1,0), Point(1,3), Point(3,3), Point(3,0), Point(1,0)))
      l.crosses(p) should be (true)
    }

    it ("should cross a MultiPoint") {
      val l = LineString(Point(0,0), Point(3,3))
      val p1 = Point(2,2)
      val p2 = Point(4, 10)
      val mp = MultiPoint(p1, p2)
      l.crosses(mp) should be (true)
    }

    it ("should overlap a LineString") {
      val l1 = LineString(Point(0,0), Point(3,3))
      val l2 = LineString(Point(1,1), Point(4,4))
      l1.overlaps(l2) should be (true)
    }

    it ("should overlap a MultiLineString") {
      val l1 = LineString(Point(0,0), Point(3,3))
      val l2 = LineString(Point(1,1), Point(4,4))
      val l3 = LineString(Point(0,10), Point(10,10))
      val ml = MultiLineString(l2, l3)
      l1.overlaps(ml) should be (true)
    }

    it ("should touch a Point") {
      val l = LineString(Point(0,0), Point(3,3))
      val p = Point(0,0)
      l.touches(p) should be (true)
    }

    it ("should touch a MultiPoint") {
      val l = LineString(Point(0,0), Point(3,3))
      val p1 = Point(3,3)
      val p2 = Point(4, 10)
      val mp = MultiPoint(p1, p2)
      l.touches(mp) should be (true)
    }

    it ("should touch a LineString") {
      val l1 = LineString(Point(0,0), Point(2,2))
      val l2 = LineString(Point(0,0), Point(0,2))
      l1.touches(l2) should be (true)
    }

    it ("should touch a Polygon") {
      val l = LineString(Point(0,4), Point(4,4))
      val p = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l.touches(p) should be (true)
    }

    it ("should be within a LineString") {
      val l1 = LineString(Point(1,1), Point(2,2))
      val l2 = LineString(Point(0,0), Point(3,3))
      l1.within(l2) should be (true)
    }

    it ("should be within a Polygon") {
      val l = LineString(Point(1,1), Point(3,1))
      val p = Polygon(LineString(Point(0,0), Point(0,4), Point(4,4), Point(4,0), Point(0,0)))
      l.within(p) should be (true)
    }

    it ("should maintain immutability over normalization") {
      val l = LineString(Point(30,20), Point(10,10), Point(20,20), Point(30,30), Point(20,10))
      val expected = l.copy
      l.normalized
      l.equals(expected) should be (true)
    }

    it ("should maintain immutability over vertices") {
      val l = LineString(Point(1,1), Point(3,1))
      val expected = l.copy
      val coord = l.points(0).getCoordinate()
      val newCoord = Point(5,5).getCoordinate()
      coord.setCoordinate(newCoord)
      l.equals(expected) should be (true)
    }
  }
}
