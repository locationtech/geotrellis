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
import com.vividsolutions.jts.{geom => jts}
import spray.json._
import spray.json.DefaultJsonProtocol._

class JtsGeometryFormatsSpec extends FlatSpec with Matchers with GeoJsonSupport {

  val point = Point(6.0,1.2).jtsGeom
  val line = Line(Point(1,2) :: Point(1,3) :: Nil).jtsGeom

  "GeometryFormats" should "know about Points" in {
    val body: JsValue =
      """{
        |  "type": "Point",
        |  "coordinates": [6.0, 1.2]
        |}""".stripMargin.parseJson

    point.toJson should equal (body)
    body.convertTo[jts.Point] should equal (point)
  }

  it should "know about MultiPoints" in {
    val mp =
      MultiPoint(List(Point(0,0), Point(0,1))).jtsGeom

    val body =
      """{
        |  "type": "MultiPoint",
        |  "coordinates": [[0.0, 0.0], [0.0, 1.0]]
        |}""".stripMargin.parseJson

    mp.toJson should equal (body)
    body.convertTo[jts.MultiPoint] should equal (mp)
  }

  it should "handle 3d points by discarding the z-coordinate" in {
    val mp =
      MultiPoint(List(Point(0,0), Point(0,1))).jtsGeom
    val mpGJ =
      """{
        |  "type": "MultiPoint",
        |  "coordinates": [[0.0, 0.0, 3.0], [0.0, 1.0, 4.0]]
        |}""".stripMargin.parseJson

    val ml =
      MultiLine(Line(Point(0,0), Point(0,1)) :: Line(Point(1,0), Point(1,1)) :: Nil).jtsGeom
    val mlGJ =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0, 1.0], [0.0, 1.0, 0.0]], [[1.0, 0.0, 2.0], [1.0, 1.0, -1.0]]]
        |}""".stripMargin.parseJson

    mlGJ.convertTo[jts.MultiLineString] should equal (ml)
    mpGJ.convertTo[jts.MultiPoint] should equal (mp)
  }

  it should "know about Lines" in {
    val body =
      """{
        |  "type": "LineString",
        |  "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |}""".stripMargin.parseJson

    line.toJson should equal (body)
    body.convertTo[jts.LineString] should equal (line)
  }

  it should "know about MultiLines" in {
    val ml =
      MultiLine(Line(Point(0,0), Point(0,1)) :: Line(Point(1,0), Point(1,1)) :: Nil).jtsGeom
    val body =
      """{
        |  "type": "MultiLineString",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0]], [[1.0, 0.0], [1.0, 1.0]]]
        |}""".stripMargin.parseJson

    ml.toJson should equal (body)
    body.convertTo[jts.MultiLineString] should equal (ml)
  }

  it should "know about Polygons" in {
    val polygon =
      Polygon(
        Line(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      ).jtsGeom
    val body =
      """{
        |  "type": "Polygon",
        |  "coordinates": [[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]]
        |}""".stripMargin.parseJson

    polygon.toJson should equal (body)
    body.convertTo[jts.Polygon] should equal (polygon)
  }

  it should "know about MultiPolygons" in {
    val mp = MultiPolygon(
      Polygon(
        Line(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      ),
      Polygon(
        Line(Point(1,1), Point(1,2), Point(2,2), Point(1,1))
      )
    ).jtsGeom

    val body =
      """{
        |  "type": "MultiPolygon",
        |  "coordinates": [[[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]], [[[1.0, 1.0], [1.0, 2.0], [2.0, 2.0], [1.0, 1.0]]]]
        |}""".stripMargin.parseJson

    mp.toJson should equal (body)
    body.convertTo[jts.MultiPolygon] should equal (mp)
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

    val gc: jts.GeometryCollection = GeometryCollection(List(Point(point), Line(line))).jtsGeom
    gc.toJson should equal (body)
    body.convertTo[jts.GeometryCollection] should equal (gc)
  }

  it should "read a Feature as if it was a Geometry" in {
    val line = Line(Point(1,2) :: Point(1,3) :: Nil).jtsGeom
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
    body.convertTo[jts.LineString] should equal(line)
  }

  it should "read a polygon with z coordinates" in {
    val body =
      """{
        |  "type": "Polygon",
        |  "coordinates": [
        |    [[0.0, 1.0, 2.0],
        |     [1.0, 2.0, 3.0],
        |     [2.0, 3.0, 4.0],
        |     [3.0, 2.0, 3.0],
        |     [4.0, 0.0, 1.0],
        |     [0.0, 1.0, 2.0]]
        |   ]
        |}""".stripMargin.parseJson

    val poly = body.convertTo[jts.Polygon]
    for(coord <- poly.getCoordinates) {
      withClue(s"Coordinate $coord") {
        coord.z should be (coord.y + 1)
      }
    }
  }
}
