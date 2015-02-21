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

package geotrellis.vector.spec

import geotrellis.vector._

import com.vividsolutions.jts.{geom=>jts}

import org.scalatest._

class MultiPolygonSpec extends FunSpec with Matchers {
  describe("MultiPolygon") {
    it ("should maintain immutability over normalization") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(-10,10), Point(-10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(11,17), Point(11,18), Point(14,17), Point(11,17)))
      val mp = MultiPolygon(p1, p2)
      val norm = mp.normalized

      mp.jtsGeom.eq(norm.jtsGeom) should be (false)
    }

    it ("should maintain immutability over polygons") {
      val p1 = Polygon(Line(Point(0,0), Point(0,10), Point(-10,10), Point(-10,0), Point(0,0)))
      val p2 = Polygon(Line(Point(11,17), Point(11,18), Point(14,17), Point(11,17)))
      val mp, expected = MultiPolygon(p1, p2)
      
      val coords = mp.polygons(0).jtsGeom.getCoordinates()
      coords(0).setCoordinate(coords(1))
      mp should be (expected)
    }
  }
}
