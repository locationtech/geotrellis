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

class SeqMethodsSpec extends AnyFunSpec with Matchers {
  describe("SeqMethods") {

    // it("should implicitly cast Seq[LineString] to MultiLineString for union") {
    //   val lines = Seq(LineString((0.0, 0.0), (1.0, 1.0)),
    //                   LineString((1.0, 1.0), (1.2, 1.2)))

    //   val union = lines.unionGeometries
    //   val expected = MultiLineStringResult(lines)
    //   union should be (expected)
    // }

    // it("should implicitly cast Seq[Point] to MultiPoint for union") {
    //   val points = Seq(Point((0.0, 0.0)), Point((1.0, 1.0)),
    //                    Point((1.5, 1.5)), Point((2.0, 2.0)))

    //   val union = points.unionGeometries
    //   val expected = MultiPointResult(points)
    //   union should be (expected)
    // }

    // it("should implicitly cast Seq[Polygon] for MultiPolygon union") {
    //   val poly1 = Polygon(LineString((0, 0), (1, 0), (1, 1), (0, 1), (0, 0)))
    //   val poly2 = Polygon(LineString((0, 0), (0, 1), (1, 1), (1, 0), (0, 0)))
    //   val polygons = Seq(poly1, poly2)

    //   val union = polygons.unionGeometries
    //   val expected = PolygonResult(poly1)
    //   union should be (expected)
    // }

    it("should handle empty sequences for polygon cascading union") {
      val polygons: Seq[Polygon] = Seq()

      val union = polygons.unionGeometries
      val expected = NoResult
      union should be (expected)
    }

    // it("should implicitly cast Seq[MultiPoint] to flattened MultiPoint for union") {
    //   val p1 = Point(0.0, 0.0)
    //   val p2 = Point(1.0, 1.0)
    //   val p3 = Point(2.0, 2.0)
    //   val points = Seq(p1, p2, p3)

    //   val mp1 = MultiPoint(p1, p2)
    //   val mp2 = MultiPoint(p2, p3)
    //   val multipoints = Seq(mp1, mp2)

    //   val union = multipoints.unionGeometries
    //   val expected = MultiPointResult(points)
    //   union should be (expected)
    // }

    // it("should implicitly cast Seq[MultiLineString] to flattened MultiLineString for union") {
    //   val l1 = LineString((0.0, 0.0), (1.0, 1.0))
    //   val l2 = LineString((1.0, 1.0), (1.2, 1.2))
    //   val l3 = LineString((1.3, 1.3), (1.4, 1.4))
    //   val lines = Seq(l1, l2, l3)

    //   val ml1 = MultiLineString(l1, l2)
    //   val ml2 = MultiLineString(l1, l3)
    //   val multilines = Seq(ml1, ml2)

    //   val union = multilines.unionGeometries
    //   val expected = MultiLineStringResult(lines)
    //   union should be (expected)
    // }

    // it("should implicitly cast Seq[MultiPolygon] to flattened MultiPolygon for union") {
    //   val poly1 = Polygon(LineString((0, 0), (1, 0), (1, 1), (0, 1), (0, 0)))
    //   val poly2 = Polygon(LineString((0, 0), (0, 1), (1, 1), (1, 0), (0, 0)))
    //   val poly3 = Polygon(LineString((0, 0), (0, 1), (2, 2), (1, 0), (0, 0)))
    //   val uniquePolygons = Seq(poly1, poly3)

    //   val mpoly1 = MultiPolygon(poly1, poly2)
    //   val mpoly2 = MultiPolygon(poly2, poly3)
    //   val multipolys = Seq(mpoly1, mpoly2)

    //   val union = multipolys.unionGeometries
    //   val expected = MultiPolygonResult(uniquePolygons)
    // }
  }
}
