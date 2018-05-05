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


class GeometryToSimpleFeatureMethodsSpec
    extends FunSpec
    with Matchers {

  describe("The .toSimpleFeature Extension Methods") {

    val point = Point(0, 1)
    val line = Line(Point(0, 0), Point(3, 3))
    val polygon = Polygon(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiPoint = MultiPoint(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiLine = MultiLine(Line(Point(0, 0), Point(4, 0)), Line(Point(0, 3), Point(0, 0)))
    val multiPolygon = MultiPolygon(
      Polygon(Point(0, 0), Point(-4, 0), Point(0, -3), Point(0, 0)),
      Polygon(Point(0, 0), Point(5, 0), Point(0, 12), Point(0, 0))
    )

    val crs = LatLng
    val emptyList = List.empty[(String, Any)]
    val nonEmptyList = List[(String, Any)](("count" -> 42))
    val map = nonEmptyList.toMap

    it("should work on Points w/ no arguments") {
      val actual = point.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(point, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Points w/ CRS") {
      val actual = point.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(point, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Points w/ Map") {
      val actual = point.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(point, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Points w/ CRS and Map") {
      val actual = point.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(point, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

    /* --------------------------------- */

    it("should work on Lines w/ no arguments") {
      val actual = line.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(line, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Lines w/ CRS") {
      val actual = line.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(line, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Lines w/ Map") {
      val actual = line.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(line, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Lines w/ CRS and Map") {
      val actual = line.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(line, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

    /* --------------------------------- */

    it("should work on Polygons w/ no arguments") {
      val actual = polygon.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(polygon, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Polygons w/ CRS") {
      val actual = polygon.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(polygon, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Polygons w/ Map") {
      val actual = polygon.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(polygon, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on Polygons w/ CRS and Map") {
      val actual = polygon.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(polygon, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

    /* --------------------------------- */

    it("should work on MultiPoints w/ no arguments") {
      val actual = multiPoint.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(multiPoint, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPoints w/ CRS") {
      val actual = multiPoint.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(multiPoint, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPoints w/ Map") {
      val actual = multiPoint.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(multiPoint, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPoints w/ CRS and Map") {
      val actual = multiPoint.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(multiPoint, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

    /* --------------------------------- */

    it("should work on MultiLines w/ no arguments") {
      val actual = multiLine.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(multiLine, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiLines w/ CRS") {
      val actual = multiLine.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(multiLine, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiLines w/ Map") {
      val actual = multiLine.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(multiLine, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiLines w/ CRS and Map") {
      val actual = multiLine.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(multiLine, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

    /* --------------------------------- */

    it("should work on MultiPolygons w/ no arguments") {
      val actual = multiPolygon.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(multiPolygon, None, emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPolygons w/ CRS") {
      val actual = multiPolygon.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(multiPolygon, Some(crs), emptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPolygons w/ Map") {
      val actual = multiPolygon.toSimpleFeature(map, "test_id")
      val expected = GeometryToSimpleFeature(multiPolygon, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work on MultiPolygons w/ CRS and Map") {
      val actual = multiPolygon.toSimpleFeature(crs, map, "test_id")
      val expected = GeometryToSimpleFeature(multiPolygon, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }
  }

}
