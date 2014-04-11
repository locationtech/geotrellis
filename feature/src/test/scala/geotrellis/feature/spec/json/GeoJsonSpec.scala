package geotrellis.feature.spec.json

import org.scalatest._
import geotrellis.feature._
import geotrellis.feature.json._

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

    GeoJson.parseGeometry(json) should equal (expected)
    json.parseGeometry should equal (expected)
  }

  it should "parse from string to simple Feature" in {
    val json = """{"type":"Feature","geometry":{"type":"Point","coordinates":[1.0,1.0]},"properties":"Data"}"""
    val expected = PointFeature(Point(1,1), "Data")

    GeoJson.parseFeature[String](json) should equal (expected)
    json.parseFeature[String] should equal (expected)
  }
}
