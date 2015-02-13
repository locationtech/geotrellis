package geotrellis.vector.json

import geotrellis.vector._
import geotrellis.testkit.vector._

import spray.json.DeserializationException
import spray.json.DefaultJsonProtocol._

import org.scalatest._

class GeoJsonSpec extends FlatSpec with Matchers {
  "GeoJson package" should "go from Geometry to String" in {
    val g = Point(1,1)

    g.toGeoJson should equal ("""{"type":"Point","coordinates":[1.0,1.0]}""")
  }

  it should "go from simple Feature to String" in {
    val f = PointFeature(Point(1,1), "Data")
    f.toGeoJson should equal ("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":"Data"}""")
  }

  it should "go from simple Feature[Int] to String" in {
    val f = PointFeature(Point(1,1), 1)
    f.toGeoJson should equal ("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":1}""")
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

    GeoJson.parse[Feature[String]](json) should equal (expected)
    GeoJson.parse[PointFeature[String]](json) should equal (expected)
  }

  it should "parse string to points" in {
    case class DataBox(data: Int)

    implicit val boxFormat = jsonFormat1(DataBox)

    val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{ "data" : 291 }},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties": { "data": 1273 }}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox]

    points.size should be (2)
  }

  it should "parse string to points and back again" in {
    val json="""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]}},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]}}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPoints.sortBy(_.x).toSeq

    points.toGeoJson.parseGeoJson[JsonFeatureCollection].getAllPoints.sortBy(_.x).toSeq should be (points)
  }

  it should "parse string to point features and back again" in {
    case class DataBox(data: Int)
    implicit val boxFormat = jsonFormat1(DataBox)
    val json="""{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{"data":291}},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties":{"data":1273}}]}"""

    val points = json.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox].sortBy(_.data.data).toSeq

    points.toGeoJson.parseGeoJson[JsonFeatureCollection].getAllPointFeatures[DataBox].sortBy(_.data.data).toSeq should be (points)
  }

  it should "serialize a Seq[MultiPolygonFeature[Int]] to GeoJson" in {
    def rect(c: Double): Polygon =
      Rectangle().setCenter((c, c)).withWidth(1.0).withHeight(1.0)
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

    intercept[DeserializationException] {
      GeoJson.parse[LineFeature[String]](json) should equal(expected)
    }
  }

  it should "parse from string with custom data without fuss" in {
    case class SomeData(name: String, value: Double)
    implicit val someDataFormat = jsonFormat2(SomeData)

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
    val expected = """{"type":"FeatureCollection","features":[{"type":"Polygon","coordinates":[[[1.0,2.0],[1.0,4.0],[3.0,4.0],[3.0,2.0],[1.0,2.0]]]}]}"""
    val featureCollection = extentGeoJson.parseGeoJson[JsonFeatureCollection]

    extentGeoJson should be (expected)
    featureCollection.getAllPolygons() match {
      case Vector(shape) => shape shouldBe a [Polygon]
      case shape => shape shouldBe a [Polygon]
    }
  }

  it should "parse geojson with IDs on custom data" in {
    case class DataBox(data: Int)
    implicit val boxFormat = jsonFormat1(DataBox)
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
    case class DataBox(data: Int)
    implicit val boxFormat = jsonFormat1(DataBox)
    val json = """{"type":"FeatureCollection","features":[{"type":"Feature","geometry":{"type":"Point","coordinates":[2674010.3642432094,264342.94293908775]},"properties":{ "data" : 291 }},{"type":"Feature","geometry":{"type":"Point","coordinates":[2714118.684319839,263231.3878492862]},"properties": { "data": 1273 }}]}"""

    intercept[DeserializationException] {
      json.parseGeoJson[JsonFeatureCollectionMap].getAllPointFeatures[DataBox]
    }
  }

}

