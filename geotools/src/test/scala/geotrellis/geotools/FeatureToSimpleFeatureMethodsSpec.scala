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

import geotrellis.proj4.WebMercator
import geotrellis.vector._

import org.scalatest._


object FeatureToSimpleFeatureMethodsSpec {
  case class Foo(x: Int, y: String)
  implicit def fooToSeq(foo: Foo): Seq[(String, Any)] = List((foo.y, foo.x))
}

class FeatureToSimpleFeatureMethodsSpec
    extends FunSpec
    with Matchers {

  import FeatureToSimpleFeatureMethodsSpec._

  describe("The .toSimpleFeature Extension Methods") {

    val point = Point(0, 1)
    val line = Line(Point(0, 0), Point(3, 3))
    val polygon = Polygon(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiPoint = MultiPoint(Point(0, 0), Point(4, 0), Point(0, 3), Point(0, 0))
    val multiLine = MultiLine(Line(Point(0, 0), Point(4, 0)), Line(Point(0, 3), Point(0, 0)))
    val multiPolygon = MultiPolygon(Polygon(Point(0, 0), Point(5, 0), Point(0, 12), Point(0, 0)))

    val crs = WebMercator
    val nonEmptyList: Seq[(String, Any)] = List(("72", 42))

    it("should work with Points") {
      val feature = Feature(point, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with Lines") {
      val feature = Feature(line, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with Polygons") {
      val feature = Feature(polygon, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with MultiPoints") {
      val feature = Feature(multiPoint, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with MultiLines") {
      val feature = Feature(multiLine, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with MultiPolygons") {
      val feature = Feature(multiPolygon, Foo(42, "72"))
      val actual = feature.toSimpleFeature("test_id")
      val expected = GeometryToSimpleFeature(feature.geom, None, nonEmptyList, "test_id")
      actual should be (expected)
    }

    it("should work with supplied CRS") {
      val feature = Feature(multiPolygon, Foo(42, "72"))
      val actual = feature.toSimpleFeature(crs, "test_id")
      val expected = GeometryToSimpleFeature(feature.geom, Some(crs), nonEmptyList, "test_id")
      actual should be (expected)
    }

  }
}
