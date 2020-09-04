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

package geotrellis.vector.io.json

import io.circe.syntax._
import cats.syntax.either._

import geotrellis.vector._

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class GeometryFormatsSpec extends AnyFlatSpec with Matchers with GeoJsonSupport {

  val point = Point(6.0,1.2)
  val line = LineString(Point(1,2) :: Point(1,3) :: Nil)
  val ml =
    MultiLineString(LineString(Point(0,0), Point(0,1)) :: LineString(Point(1,0), Point(1,1)) :: Nil)
  val mpoint =
    MultiPoint(List(Point(0,0), Point(0,1)))
  val mp = MultiPolygon(
    Polygon(
      LineString(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
    ),
    Polygon(
      LineString(Point(1,1), Point(1,2), Point(2,2), Point(1,1))
    )
  )
  
  "GeometryFormats" should "know about Points" in {
    val body =
      """{
        |  "type": "Point",
        |  "coordinates": [6.0, 1.2]
        |}""".stripMargin.parseJson

    point.asJson should equal (body)
    body.as[Point].valueOr(throw _) should equal (point)
  }

  it should "know about MultiPoints" in {

    val body =
      """{
        |  "type": "MultiPoint",
        |  "coordinates": [[0.0, 0.0], [0.0, 1.0]]
        |}""".stripMargin.parseJson

    mpoint.asJson should equal (body)
    body.as[MultiPoint].valueOr(throw _) should equal (mpoint)
  }

  it should "handle 3d points by discarding the z-coordinate" in {
    val mp =
      MultiPoint(List(Point(0,0), Point(0,1)))
    val mpGJ =
      """{
        |  "type": "MultiPoint",
        |  "coordinates": [[0.0, 0.0, 3.0], [0.0, 1.0, 4.0]]
        |}""".stripMargin.parseJson

    val ml =
      MultiLineString(LineString(Point(0,0), Point(0,1)) :: LineString(Point(1,0), Point(1,1)) :: Nil)
    val mlGJ =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0, 1.0], [0.0, 1.0, 0.0]], [[1.0, 0.0, 2.0], [1.0, 1.0, -1.0]]]
        |}""".stripMargin.parseJson

    mlGJ.as[MultiLineString].valueOr(throw _) should equal (ml)
    mpGJ.as[MultiPoint].valueOr(throw _) should equal (mp)
  }

  it should "know about LineStrings" in {
    val body =
      """{
        |  "type": "LineString",
        |  "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |}""".stripMargin.parseJson

    line.asJson should equal (body)
    body.as[LineString].valueOr(throw _) should equal (line)
  }

  it should "know about MultiLineStrings" in {
    val body =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0]], [[1.0, 0.0], [1.0, 1.0]]]
        |}""".stripMargin.parseJson

    ml.asJson should equal (body)
    body.as[MultiLineString].valueOr(throw _) should equal (ml)
  }

  it should "know about Polygons" in {
    val polygon =
      Polygon(
        LineString(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      )
    val body =
      """{
        |  "type": "Polygon",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]]
        |}""".stripMargin.parseJson

    polygon.asJson should equal (body)
    body.as[Polygon].valueOr(throw _) should equal (polygon)
  }

  it should "know about MultiPolygons" in {
    val body =
      """{
        |  "type": "MultiPolygon",
        |  "coordinates": [[[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]], [[[1.0, 1.0], [1.0, 2.0], [2.0, 2.0], [1.0, 1.0]]]]
        |}""".stripMargin.parseJson

    mp.asJson should equal (body)
    body.as[MultiPolygon].valueOr(throw _) should equal (mp)
  }


  it should "know how to collection" in {
    val body =
      """{
        |  "type": "GeometryCollection",
        |  "geometries": [{
        |    "type": "Point",
        |    "coordinates": [6.0, 1.2]
        |  }, {
        |    "type": "LineString",
        |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |  }]
        |}""".stripMargin.parseJson

    val gc: GeometryCollection = GeometryCollection(List(point, line))
    gc.asJson should equal (body)
    body.as[GeometryCollection].valueOr(throw _) should equal (gc)
  }
  
  it should "know how to collection of collections" in {
    val body =
      """{
        |  "type": "GeometryCollection",
        |  "geometries": [{
        |    "type": "MultiLineString",
        |    "coordinates": [[[0.0, 0.0], [0.0, 1.0]], [[1.0, 0.0], [1.0, 1.0]]]
        |  }, {
        |    "type": "MultiPoint",
        |    "coordinates": [[0.0, 0.0], [0.0, 1.0]]
        |  }, {
        |    "type": "MultiPolygon",
        |    "coordinates": [[[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]], [[[1.0, 1.0], [1.0, 2.0], [2.0, 2.0], [1.0, 1.0]]]]
        |  }]
        |}""".stripMargin.parseJson

    val gc: GeometryCollection = GeometryCollection(List(ml,mpoint,mp))
    gc.asJson should equal (body)
    body.as[GeometryCollection].valueOr(throw _) should equal (gc)
  }

  it should "read a Feature as if it was a Geometry" in {
    val line = LineString(Point(1,2) :: Point(1,3) :: Nil);
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "LineString",
        |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |  },
        |  "properties": 321
        |}""".stripMargin.parseJson

    //I test it only for a LineString, but the logic works for all Geomtries
    body.as[LineString].valueOr(throw _) should equal(line)
  }
}
