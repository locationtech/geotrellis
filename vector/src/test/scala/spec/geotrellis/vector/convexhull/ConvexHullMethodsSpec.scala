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

package geotrellis.vector.convexhull

import geotrellis.vector._
import geotrellis.vector.testkit._

import org.scalatest._

class ConvexHullMethodsSpec extends FunSpec with Matchers {
  describe("ConvexHullMethods") {
    it("should return the correct polygon for a MultiPoint") {
      val mp = MultiPoint(Point(-1,0), Point(-0.5,0.5), Point(-1, 1), Point(1, 1), Point(1, 0))
      mp.convexHull match {
        case PolygonResult(p) =>
          p should matchGeom(Polygon(Point(-1,0), Point(-1, 1), Point(1, 1), Point(1, 0), Point(-1,0)), 0.0)
        case x =>
          mp.convexHull should be (a[PolygonResult])
      }
    }

    it("should return the correct polygon for a three point MultiPoint") {
      val mp = MultiPoint(Point(-1, 0), Point(-0.5, 0.5), Point(-1, 1))
      mp.convexHull match {
        case PolygonResult(p) =>
          p should matchGeom(Polygon(Point(-1, 0), Point(-0.5, 0.5), Point(-1,1), Point(-1, 0)), 0.0)
        case x =>
          mp.convexHull should be (a[PolygonResult])
      }
    }

    it("should return the correct polygon for a two-point MultiPoint") {
      val mp = MultiPoint(Point(-1,0), Point(-1, 1))
      val result = mp.convexHull
      result should be (NoResult)
    }

    it("should return the correct polygon for a Point") {
      val mp = Point(-1,0)
      val result = mp.convexHull
      result should be (NoResult)
    }

    it("should return the correct polygon for a Polygon with a dent") {
      val p = Polygon(Point(-1,0), Point(-0.5,0.5), Point(-1, 1), Point(1, 1), Point(1, 0), Point(-1, 0))
      p.convexHull match {
        case PolygonResult(p) =>
          p should matchGeom(Polygon(Point(-1,0), Point(-1, 1), Point(1, 1), Point(1, 0), Point(-1,0)), 0.0)
        case x =>
          p.convexHull should be (a[PolygonResult])
      }
    }
  }
}
