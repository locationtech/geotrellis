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

import org.scalatest._
import geotrellis.vector._
import geotrellis.vector.io._

import spray.json._
import spray.json.DefaultJsonProtocol._

class FeatureFormatsSpec extends FlatSpec with Matchers with GeoJsonSupport {

  val pointFeature = PointFeature(Point(6.0,1.2), 123)
  val lineFeature = LineFeature(Line(Point(1,2) :: Point(1,3) :: Nil), 321)

  "Feature" should "work single point feature" in {
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "Point",
        |    "coordinates": [6.0, 1.2]
        |  },
        |  "properties": 123
        |}""".stripMargin.parseJson

    pointFeature.toJson should equal (body)
    body.convertTo[PointFeature[Int]] should equal(pointFeature)
  }

  it should "work single line feature" in {
    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "LineString",
        |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |  },
        |  "properties": 321
        |}""".stripMargin.parseJson

    lineFeature.toJson should equal (body)
    body.convertTo[LineFeature[Int]] should equal(lineFeature)
  }

  it should "knows how to heterogeneous collection" in {
    val body =
     """{
       |  "type": "FeatureCollection",
       |  "features": [{
       |    "type": "Feature",
       |    "geometry": {
       |      "type": "LineString",
       |      "coordinates": [[1.0, 2.0], [1.0, 3.0]]
       |    },
       |    "properties": 321
       |  }, {
       |    "type": "Feature",
       |    "geometry": {
       |      "type": "Point",
       |      "coordinates": [6.0, 1.2]
       |    },
       |    "properties": 123
       |  }]
       |}""".stripMargin.parseJson

    val jsonFeatures = new JsonFeatureCollection()
    jsonFeatures += lineFeature
    jsonFeatures += pointFeature

    jsonFeatures.toJson should equal (body)

    val fc = body.convertTo[JsonFeatureCollection]
    fc.getAllFeatures[PointFeature[Int]] should contain (pointFeature)
    fc.getAllFeatures[LineFeature[Int]] should contain (lineFeature)
  }

  it should "parse polygons out of a feature collection" in {
    val geojson =
      """
{
    "type": "FeatureCollection",
    "features": [
        {
            "type": "Feature",
            "properties": {},
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [
                            -115.40039062500001,
                            37.71859032558816
                        ],
                        [
                            -115.40039062500001,
                            42.391008609205045
                        ],
                        [
                            -105.99609375000001,
                            42.391008609205045
                        ],
                        [
                            -105.99609375000001,
                            37.71859032558816
                        ],
                        [
                            -115.40039062500001,
                            37.71859032558816
                        ]
                    ]
                ]
            }
        },
        {
            "type": "Feature",
            "properties": {},
            "geometry": {
                "type": "Polygon",
                "coordinates": [
                    [
                        [
                            -98.21777343750001,
                            38.47939467327645
                        ],
                        [
                            -98.21777343750001,
                            41.27780646738185
                        ],
                        [
                            -90.6591796875,
                            41.27780646738185
                        ],
                        [
                            -90.6591796875,
                            38.47939467327645
                        ],
                        [
                            -98.21777343750001,
                            38.47939467327645
                        ]
                    ]
                ]
            }
        }
    ]
}"""

    val features = geojson.parseGeoJson[JsonFeatureCollection].getAllPolygons()
    features.length should be (2)
  }

  it should "be able to handle Feature with custom data" in {
    case class SomeData(name: String, value: Double)
    val format = jsonFormat2(SomeData)
    implicit val someDataFormat: JsonReader[SomeData] = format

    val f = PointFeature(Point(1,44), SomeData("Bob", 32.2))

    val body =
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "Point",
        |    "coordinates": [1.0, 44.0]
        |  },
        |  "properties": {
        |    "name": "Bob",
        |    "value": 32.2
        |  }
        |}""".stripMargin.parseJson

    implicit val _format: JsonFormat[SomeData] = format
    f.toJson should equal (body)
    body.convertTo[PointFeature[SomeData]] should equal (f)
  }
}
