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

import io.circe._
import io.circe.generic._
import io.circe.syntax._

import geotrellis.vector._
import geotrellis.vector.testkit._

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec

class GeoJsonSpec extends AnyFlatSpec with Matchers {
  "GeoJson package" should "go from Geometry to String" in {
    val g = Point(1,1)

    g.toGeoJson should equal ("""{"type":"Point","coordinates":[1.0,1.0]}""")
  }

  it should "go from simple Feature to String" in {
    val f = PointFeature(Point(1,1), "Data")
    f.toGeoJson should equal ("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"bbox":[1.0,1.0,1.0,1.0],"properties":"Data"}""")
  }

  it should "go from simple Feature[Int] to String" in {
    val f = PointFeature(Point(1,1), 1)
    f.toGeoJson should equal ("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"bbox":[1.0,1.0,1.0,1.0],"properties":1}""")
  }

  it should "parse from string to Geometry" in {
    val json = """{"type":"Point","coordinates":[1.0,1.0]}"""
    val expected = Point(1,1)

    GeoJson.parse[Geometry](json) should equal (expected)
    GeoJson.parse[Point](json) should equal (expected)
  }

  it should "parse from string to simple Feature" in {
    val json = """{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":"Data"}"""
    val expected = PointFeature(Point(1,1), "Data")

    GeoJson.parse[PointFeature[String]](json) should equal (expected)
  }

  it should "parse string to points" in {
    @JsonCodec
    case class DataBox(data: Int)

    val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{ "data" : 291 }},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties": { "data": 1273 }}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox]

    points.size should be (2)
  }

  it should "parse string to points and back again" in {
    val json="""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]}},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]}}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPoints.sortBy(_.x).toSeq

    points.toGeoJson.parseGeoJson[GeometryCollection].getAll[Point] should be (points)

  }

  it should "parse string to point features and back again" in {
    @JsonCodec
    case class DataBox(data: Int)
    val json="""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{"data":291}},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties":{"data":1273}}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox].sortBy(_.data.data).toSeq

    points.toGeoJson.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox].sortBy(_.data.data).toSeq should be (points.toVector)
  }

  it should "serialize a Seq[MultiPolygonFeature[Int]] to GeoJson" in {
    def rect(c: Double): Polygon =
      Rectangle().setCenter(Point(c, c)).withWidth(1.0).withHeight(1.0)
    val mp = Seq(
      MultiPolygonFeature(MultiPolygon(rect(0.0), rect(5.0)), 3),
      MultiPolygonFeature(MultiPolygon(rect(1.0), rect(4.0)), 5)
    )
    val json = mp.toGeoJson
    json.parseGeoJson[JsonFeatureCollection].getAllMultiPolygonFeatures[Int].sortBy(_.data).toSeq should be (mp)
  }

  it should "fail when you ask for the wrong feature" in {
    val json = """{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":"Data"}"""
    val expected = PointFeature(Point(1,1), "Data")

    intercept[DecodingFailure] {
      GeoJson.parse[LineStringFeature[String]](json) should equal(expected)
    }
  }

  it should "parse from string with custom data without fuss" in {
    @JsonCodec
    case class SomeData(name: String, value: Double)

    val jsonFeature =
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
        |}""".stripMargin
    val expected = PointFeature(Point(1,44), SomeData("Bob", 32.2))

    jsonFeature.parseGeoJson[PointFeature[SomeData]] should equal (expected)
  }

  it should "convert from Extent to geojson on demand" in {
    val extent = Extent(1.0, 2.0, 3.0, 4.0)
    val extentGeoJson = extent.toGeoJson
    val expected = """{"type":"Polygon","coordinates":[[[1.0,2.0],[1.0,4.0],[3.0,4.0],[3.0,2.0],[1.0,2.0]]]}"""

    extentGeoJson should be (expected)
  }

  it should "parse geojson with IDs on custom data" in {
    @JsonCodec
    case class DataBox(data: Int)
    val json = """{
                 |  "type":"FeatureCollection",
                 |  "features":[
                 |    {"type":"Feature","geometry":{"type":"Point","coordinates":[10.34,22.75]},"properties":{"data" : 291},"id":"jackson5"},
                 |    {"type":"Feature","geometry":{"type":"Point","coordinates":[18.69,23.862]},"properties":{"data": 1273},"id":"volcano"},
                 |    {"type":"Feature","geometry":{"type":"Point","coordinates":[14.13,11.21]},"properties":{"data": 142},"id":"zorp"}
                 |  ]
                 |}""".stripMargin
    val points: Map[String, PointFeature[DataBox]] = json.parseGeoJson[JsonFeatureCollectionMap].getAllPointFeatures[DataBox]

    points.keys should be (Set("jackson5", "volcano", "zorp"))
    points.size should be (3)
  }

  it should "throw an exception in case we expect features with IDs and recieve features without IDs" in {
    @JsonCodec
    case class DataBox(data: Int)
    val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{ "data" : 291 }},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties": { "data": 1273 }}]}"""

    intercept[DecodingFailure] {
      json.parseGeoJson[JsonFeatureCollectionMap].getAllPointFeatures[DataBox]
    }
  }

  it should "convert polygons in GeoJson GeometryCollection" in  {

    val l1 = LineString(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
    val l2 = LineString(Point(1,1), Point(1,6), Point(6,6), Point(6,1), Point(1,1))

    val p1: Polygon = Polygon(l1)
    val p2: Polygon = Polygon(l2)

    val json = Seq(p1, p2).toGeoJson
    val polygonsBack = json.parseGeoJson[GeometryCollection].getAll[Polygon]

    polygonsBack should be (Seq(p1, p2))
  }

  // TODO: tear this test out and burn the code it tests
  it should "extract geometries in GeoJson from different Features, Geometries or Collections" in  {

    @JsonCodec
    case class SomeData(name: String, value: Double)

    val point1 = Point(0,0)
    val line1 = LineString(point1, Point(0,5), Point(5,5), Point(5,0), Point(0,0))
    val poly1: Polygon = Polygon(line1)

    val pointfeature1 = PointFeature(point1, SomeData("Bob", 32.2))
    val linefeature2 = LineStringFeature(line1, SomeData("Alice", 31.2))

    val jsonGeom = poly1.toGeoJson
    val jsonGeomCol = Seq(point1, line1, poly1).toGeoJson
    val jsonFeature = pointfeature1.toGeoJson
    val jsonFeatCol = Seq(pointfeature1, linefeature2).toGeoJson

    val polygonsBack = jsonGeomCol.parseGeoJson[GeometryCollection].getAll[Polygon]
    polygonsBack.toSeq should be (Seq(poly1))

    val t1 = jsonGeom.extractGeometries[Polygon]
    t1 should be (Seq(poly1))

    val t2 = jsonGeom.extractGeometries[Point]
    t2 should be (Seq())

    val t3 = jsonGeomCol.extractGeometries[Polygon]
    t3 should be (Seq(poly1))

    val t4 = jsonGeomCol.extractGeometries[MultiPoint]
    t4 should be (Seq())

    val t5 = jsonFeature.extractGeometries[Point]
    t5 should be (Seq(point1))

    val t6 = jsonFeature.extractGeometries[Polygon]
    t6 should be (Seq())

    val t7 = jsonFeatCol.extractGeometries[Point]
    t7 should be (Seq(point1))

    val t8 = jsonFeatCol.extractGeometries[Polygon]
    t8 should be (Seq())

    val t9 = jsonFeature.extractFeatures[PolygonFeature[SomeData]]
    t9 should be (Seq())

    val t10 = jsonFeature.extractFeatures[PointFeature[SomeData]]
    t10 should be (Seq(pointfeature1))

    val t11 = jsonFeature.extractFeatures[LineStringFeature[SomeData]]
    t11 should be (Seq())

    val t12 = jsonFeatCol.extractFeatures[LineStringFeature[SomeData]]
    t12 should be (Seq(linefeature2))

    val line2 = LineString(point1, point1, point1, point1, point1)
    val poly2: Polygon = Polygon(line2)
    poly2.toGeoJson.extractGeometries[Polygon].head should matchGeom (poly2)
  }

  it should "create a feature collection out of a set of features" in {
    val f1 = Feature(Polygon((10.0, 10.0), (10.0, 20.0), (30.0, 30.0), (10.0, 10.0)), Json.fromFields("value" -> 1.asJson :: Nil))
    val f2 = Feature(Polygon((-10.0, -10.0), (-10.0, -20.0), (-30.0, -30.0), (-10.0, -10.0)), Json.fromFields("value" -> 2.asJson :: Nil))

    val geoJson = Seq(f1, f2).toGeoJson
    val datas = geoJson.parseGeoJson[JsonFeatureCollection].getAllPolygonFeatures[Json]().map { f => f.data }.toSet
    datas should be (Set(f1.data, f2.data))
  }
}
