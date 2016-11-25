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
import geotrellis.vector.io._

import org.scalatest._
import spray.json._
import spray.json.DefaultJsonProtocol._

class JsonCrsSpec extends FlatSpec with Matchers with GeoJsonSupport {
  val point = Point(6.0,1.2)
  val line = Line(Point(1,2) :: Point(1,3) :: Nil)
  val crs = NamedCRS("napkin:map:sloppy")

  it should "should attach to a Geometry" in {
    val body =
        """{
          |  "type": "LineString",
          |  "coordinates": [[1.0, 2.0], [1.0, 3.0]],
          |  "crs": {
          |    "type": "name",
          |    "properties": {
          |      "name": "napkin:map:sloppy"
          |    }
          |  }
          |}""".stripMargin.parseJson

    WithCrs(line, crs).toJson should be (body)
    line.withCrs(crs).toJson should be (body)
    body.convertTo[WithCrs[Line]] should equal (WithCrs(line, crs))
  }

  it should "should attach to a GeometryCollection" in {
    val body =
        """{
          |  "type": "GeometryCollection",
          |  "geometries": [{
          |    "type": "Point",
          |    "coordinates": [6.0, 1.2]
          |  }, {
          |    "type": "LineString",
          |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
          |  }],
          |  "crs": {
          |    "type": "name",
          |    "properties": {
          |      "name": "napkin:map:sloppy"
          |    }
          |  }
          |}""".stripMargin.parseJson

    val gc = GeometryCollection(List(point, line))
    WithCrs(gc, crs).toJson should equal (body)
    body.convertTo[WithCrs[GeometryCollection]] should equal (WithCrs(gc, crs))
  }

  it should "attach to a Feature" in {
    val f = PointFeature(Point(1, 44), "Secrets")
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "Point",
        |    "coordinates": [1.0, 44.0]
        |  },
        |  "properties": "Secrets",
        |  "crs": {
        |    "type": "name",
        |    "properties": {
        |      "name": "napkin:map:sloppy"
        |    }
        |  }
        |}""".stripMargin.parseJson

    f.withCrs(crs).toJson should equal (body)
    body.convertTo[WithCrs[PointFeature[String]]] should equal (WithCrs(f, crs))
  }


  it should "decode NamedCRS with EPSG code" in {
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "Point",
        |    "coordinates": [1.0, 44.0]
        |  },
        |  "properties": "Secrets",
        |  "crs": {
        |    "type": "name",
        |    "properties": {
        |      "name": "epsg:3857"
        |    }
        |  }
        |}""".stripMargin

    val withCrs = body.parseJson.convertTo[WithCrs[PointFeature[String]]]
    withCrs.crs.toCRS.get.epsgCode should be (Some(3857))
  }
}
