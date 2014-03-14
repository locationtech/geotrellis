/**************************************************************************
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
 **************************************************************************/

package geotrellis.feature.spec

import geotrellis.feature._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest.FunSpec
import org.scalatest.matchers._

class PolygonSpec extends FunSpec with ShouldMatchers {
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

    it ("should be a rectangle if constructed with a rectangular set of points") {
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
      val p = Polygon(l, Set(h))
      p.boundary should be (MultiLineResult(Set(l, h)))
    }

    it ("should have vertices") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p = Polygon(l)
      p.vertices should be (MultiPoint(Point(0,0), Point(0,5), Point(5,5), Point(5,0)))
    }

    it ("should have a bounding box whose points are " +
        "(minx, miny), (minx, maxy), (maxx, maxy), (maxx, miny), (minx, miny).") {
      val l = Line(Point(0,0), Point(2,2), Point(4,0), Point(0,0))
      val p = Polygon(l)
      p.boundingBox should be (Polygon(Line(Point(0,0), Point(0,2), Point(4,2), Point(4,0), Point(0,0))))
    }

    it ("should have a perimeter equal to the length of its boundary") {
      val l = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val h = Line(Point(1,1), Point(1,4), Point(4,4), Point(4,1), Point(1,1))
      val p = Polygon(l, Set(h))
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
      p & mp should be (MultiPointResult(Set(p1, p2)))
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
      p1 & p2 should be (MultiPointResult(Set(Point(5,0), Point(5,5))))
    }

    it ("should intersect with a TwoDimensions and return a MultiLineResult") {
      val l1 = Line(Point(0,0), Point(0,6), Point(5,6), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(5,0), Point(5,2), Point(6,2), Point(6,4), Point(5,4),
                    Point(5,6), Point(7,6), Point(7,0), Point(5,0))
      val p2 = Polygon(l2)
      p1 & p2 should be (MultiLineResult(Set(Line(Point(5,6), Point(5,4)),
                                             Line(Point(5,2), Point(5,0)))))
    }

    it ("should intersect with a TwoDimensions and return a MultiPolygonResult") {
      val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
      val p1 = Polygon(l1)
      val l2 = Line(Point(0,4), Point(0,7), Point(5,7), Point(5,4), Point(3,4),
                    Point(3,6), Point(2,6), Point(2,4), Point(0,4))
      val p2 = Polygon(l2)
      p1 & p2 should be (MultiPolygonResult(Set(Polygon(Line(Point(0,4), Point(0,5),
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
        GeometryCollection(points = Set(Point(5,5)), polygons = Set(Polygon(Line(Point(0,4), Point(0,5),
          Point(2,5), Point(2,4), Point(0,4)))))
      val result = p1 & p2
      result match {
        case GeometryCollectionResult(gc) => gc should be (expected)
        case _ => fail()
      }
    }
  }
}
