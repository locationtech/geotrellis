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

package geotrellis.geotools

import geotrellis.proj4.LatLng
import geotrellis.vector._

import org.scalatest._


class SimpleFeatureToGeometryMethodsSpec
    extends FunSpec
    with Matchers {

  describe("The .toGeometry Extension Methods") {

    val point = Point(0, 1)
    val line = Line(Point(0, 0), Point(3, 3))
    val polygon = Polygon(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiPoint = MultiPoint(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiLine = MultiLine(Line(Point(0, 0), Point(4, 0)), Line(Point(0, 3), Point(0, 0)))
    val multiPolygon = MultiPolygon(Polygon(Point(0, 0), Point(5, 0), Point(0, 12), Point(0, 0)))

    val crs = LatLng
    val nonEmptyList = List[(String, Any)](("count" -> 42))
    val map = nonEmptyList.toMap

    it("should work on Features of Points") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      val actual: Point = simpleFeature.toGeometry[Point]
      val expected = point
      actual should be (expected)
    }

    it("should work on Features of Lines") {
      val simpleFeature = GeometryToSimpleFeature(line, Some(crs), nonEmptyList)
      val actual: Line = simpleFeature.toGeometry[Line]
      val expected = line
      actual should be (expected)
    }

    it("should work on Features of Polygons") {
      val simpleFeature = GeometryToSimpleFeature(polygon, Some(crs), nonEmptyList)
      val actual: Polygon = simpleFeature.toGeometry[Polygon]
      val expected = polygon
      actual should be (expected)
    }

    it("should work on Features of MultiPoints") {
      val simpleFeature = GeometryToSimpleFeature(multiPoint, Some(crs), nonEmptyList)
      val actual: MultiPoint = simpleFeature.toGeometry[MultiPoint]
      val expected = multiPoint
      actual should be (expected)
    }

    it("should work on Features of MultiLines") {
      val simpleFeature = GeometryToSimpleFeature(multiLine, Some(crs), nonEmptyList)
      val actual: MultiLine = simpleFeature.toGeometry[MultiLine]
      val expected = multiLine
      actual should be (expected)
    }

    it("should work on Features of MultiPolygons") {
      val simpleFeature = GeometryToSimpleFeature(multiPolygon, Some(crs), nonEmptyList)
      val actual: MultiPolygon = simpleFeature.toGeometry[MultiPolygon]
      val expected = multiPolygon
      actual should be (expected)
    }

    it("should work on Features of Geometry") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      val actual: Geometry = simpleFeature.toGeometry[Geometry]
      val expected = point
      actual should be (expected)
    }

    it("should throw in response to mis-matches") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      intercept[Exception] {
        println(simpleFeature.toGeometry[Line])
      }
    }
  }
}
