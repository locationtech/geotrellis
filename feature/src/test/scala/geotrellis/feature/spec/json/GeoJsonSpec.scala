package geotrellis.feature.json

import org.scalatest._
import geotrellis.feature._
import spray.json.DeserializationException

class GeoJsonSpec extends FlatSpec with ShouldMatchers {
  "GeoJson package" should "go from Geometry to String" in {
    val g = Point(1,1)

    g.toGeoJson should equal ("""{"type":"Point","coordinates":[1.0,1.0]}""")
  }

  it should "go from simple Feature to String" in {
    val f = PointFeature(Point(1,1), "Data")
    f.toGeoJson should equal ("""{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":"Data"}""")
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
}

