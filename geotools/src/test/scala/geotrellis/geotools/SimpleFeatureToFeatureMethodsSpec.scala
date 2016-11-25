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


object SimpleFeatureToFeatureMethodsSpec {
  case class Foo(x: Int, y: String)
  implicit def mapToFoo(map: Map[String, Any]): Foo = Foo(42, "72")
}

class SimpleFeatureToFeatureMethodsSpec
    extends FunSpec
    with Matchers {

  import SimpleFeatureToFeatureMethodsSpec._

  describe("The .toFeature Extension Methods") {

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
      val actual: Feature[Point, Map[String, Any]] = simpleFeature.toFeature[Point]
      val expected = Feature(point, map)
      actual should be (expected)
    }

    it("should work on Features of Lines") {
      val simpleFeature = GeometryToSimpleFeature(line, Some(crs), nonEmptyList)
      val actual: Feature[Line, Map[String, Any]] = simpleFeature.toFeature[Line]
      val expected = Feature(line, map)
      actual should be (expected)
    }

    it("should work on Features of Polygons") {
      val simpleFeature = GeometryToSimpleFeature(polygon, Some(crs), nonEmptyList)
      val actual: Feature[Polygon, Map[String, Any]] = simpleFeature.toFeature[Polygon]
      val expected = Feature(polygon, map)
      actual should be (expected)
    }

    it("should work on Features of MultiPoints") {
      val simpleFeature = GeometryToSimpleFeature(multiPoint, Some(crs), nonEmptyList)
      val actual: Feature[MultiPoint, Map[String, Any]] = simpleFeature.toFeature[MultiPoint]
      val expected = Feature(multiPoint, map)
      actual should be (expected)
    }

    it("should work on Features of MultiLines") {
      val simpleFeature = GeometryToSimpleFeature(multiLine, Some(crs), nonEmptyList)
      val actual: Feature[MultiLine, Map[String, Any]] = simpleFeature.toFeature[MultiLine]
      val expected = Feature(multiLine, map)
      actual should be (expected)
    }

    it("should work on Features of MultiPolygons") {
      val simpleFeature = GeometryToSimpleFeature(multiPolygon, Some(crs), nonEmptyList)
      val actual: Feature[MultiPolygon, Map[String, Any]] = simpleFeature.toFeature[MultiPolygon]
      val expected = Feature(multiPolygon, map)
      actual should be (expected)
    }

    it("should work on Features of Geometry") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      val actual: Feature[Geometry, Map[String, Any]] = simpleFeature.toFeature[Geometry]
      val expected = Feature(point, map)
      actual should be (expected)
    }

    it("should throw in response to mis-matches") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      intercept[Exception] {
        println(simpleFeature.toFeature[Line])
      }
    }

    it("should work with an implicit conversion") {
      val simpleFeature = GeometryToSimpleFeature(point, Some(crs), nonEmptyList)
      val actual: Feature[Point, Foo] = simpleFeature.toFeature[Point, Foo]
      val expected = Feature(point, Foo(42, "72"))
      actual should be (expected)
    }
  }

}
