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

import geotrellis.vector._

import org.scalatest._
import spray.json._
import spray.json.DefaultJsonProtocol._

class GeometryFormatsSpec extends FlatSpec with Matchers with GeoJsonSupport {

  val point = Point(6.0,1.2)
  val line = Line(Point(1,2) :: Point(1,3) :: Nil)

  "GeometryFormats" should "know about Points" in {
    val body: JsValue =
      """{
        |  "type": "Point",
        |  "coordinates": [6.0, 1.2]
        |}""".stripMargin.parseJson

    point.toJson should equal (body)
    body.convertTo[Point] should equal (point)
  }

  it should "know about MultiPoints" in {
    val mp =
      MultiPoint(List(Point(0,0), Point(0,1)))

    val body =
      """{
        |  "type": "MultiPoint",
        |  "coordinates": [[0.0, 0.0], [0.0, 1.0]]
        |}""".stripMargin.parseJson

    mp.toJson should equal (body)
    body.convertTo[MultiPoint] should equal (mp)
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
      MultiLine(Line(Point(0,0), Point(0,1)) :: Line(Point(1,0), Point(1,1)) :: Nil)
    val mlGJ =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0, 1.0], [0.0, 1.0, 0.0]], [[1.0, 0.0, 2.0], [1.0, 1.0, -1.0]]]
        |}""".stripMargin.parseJson

    mlGJ.convertTo[MultiLine] should equal (ml)
    mpGJ.convertTo[MultiPoint] should equal (mp)
  }

  it should "know about Lines" in {
    val body =
      """{
        |  "type": "LineString",
        |  "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |}""".stripMargin.parseJson

    line.toJson should equal (body)
    body.convertTo[Line] should equal (line)
  }

  it should "know about MultiLines" in {
    val ml =
      MultiLine(Line(Point(0,0), Point(0,1)) :: Line(Point(1,0), Point(1,1)) :: Nil)
    val body =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0]], [[1.0, 0.0], [1.0, 1.0]]]
        |}""".stripMargin.parseJson

    ml.toJson should equal (body)
    body.convertTo[MultiLine] should equal (ml)
  }

  it should "know about Polygons" in {
    val polygon =
      Polygon(
        Line(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      )
    val body =
      """{
        |  "type": "Polygon",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]]
        |}""".stripMargin.parseJson

    polygon.toJson should equal (body)
    body.convertTo[Polygon] should equal (polygon)
  }

  it should "know about MultiPolygons" in {
    val mp = MultiPolygon(
      Polygon(
        Line(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      ),
      Polygon(
        Line(Point(1,1), Point(1,2), Point(2,2), Point(1,1))
      )
    )

    val body =
      """{
        |  "type": "MultiPolygon",
        |  "coordinates": [[[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]], [[[1.0, 1.0], [1.0, 2.0], [2.0, 2.0], [1.0, 1.0]]]]
        |}""".stripMargin.parseJson

    mp.toJson should equal (body)
    body.convertTo[MultiPolygon] should equal (mp)
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
    gc.toJson should equal (body)
    body.convertTo[GeometryCollection] should equal (gc)
  }

  it should "read a Feature as if it was a Geometry" in {
    val line = Line(Point(1,2) :: Point(1,3) :: Nil);
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "LineString",
        |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |  },
        |  "properties": 321
        |}""".stripMargin.parseJson

    //I test it only for a Line, but the logic works for all Geomtries
    body.convertTo[Line] should equal(line)
  }
}
