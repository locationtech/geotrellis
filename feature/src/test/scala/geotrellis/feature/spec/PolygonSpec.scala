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
  }
}
