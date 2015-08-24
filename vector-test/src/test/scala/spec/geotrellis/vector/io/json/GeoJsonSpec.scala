package geotrellis.vector.io.json

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

    points.toGeoJson.parseGeoJson[GeometryCollection].points should be (points)

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
    val expected = """{"type":"Polygon","coordinates":[[[1.0,2.0],[1.0,4.0],[3.0,4.0],[3.0,2.0],[1.0,2.0]]]}"""

    extentGeoJson should be (expected)
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

  it should "convert polygons in GeoJson GeometryCollection" in  {

    val l1 = Line(Point(0,0), Point(0,5), Point(5,5), Point(5,0), Point(0,0))
    val l2 = Line(Point(1,1), Point(1,6), Point(6,6), Point(6,1), Point(1,1))

    val p1: Polygon = Polygon(l1)
    val p2: Polygon = Polygon(l2)

    val json = Seq(p1, p2).toGeoJson
    val polygonsBack = json.parseGeoJson[GeometryCollection].polygons

    polygonsBack should be (Seq(p1, p2))
  }

  it should "extract geometries in GeoJson from different Features, Geometries or Collections" in  {

    case class SomeData(name: String, value: Double)
    implicit val someDataFormat = jsonFormat2(SomeData)

    val point1 = Point(0,0)
    val line1 = Line(point1, Point(0,5), Point(5,5), Point(5,0), Point(0,0))
    val poly1: Polygon = Polygon(line1)

    val pointfeature1 = PointFeature(point1, SomeData("Bob", 32.2))
    val linefeature2 = LineFeature(line1, SomeData("Alice", 31.2))

    val jsonGeom = poly1.toGeoJson
    val jsonGeomCol = Seq(point1, line1, poly1).toGeoJson
    val jsonFeature = pointfeature1.toGeoJson
    val jsonFeatCol = Seq(pointfeature1, linefeature2).toGeoJson

    val polygonsBack = jsonGeomCol.parseGeoJson[GeometryCollection].polygons
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

    val t11 = jsonFeature.extractFeatures[LineFeature[SomeData]]
    t11 should be (Seq())

    val t12 = jsonFeatCol.extractFeatures[LineFeature[SomeData]]
    t12 should be (Seq(linefeature2))

    // catch java.lang.AssertionError from Polygon.scala "Empty Geometry"
    intercept[java.lang.AssertionError] {
      val line2 = Line(point1, point1, point1, point1, point1)
      val poly2: Polygon = Polygon(line2)
      poly2.toGeoJson.extractGeometries[Polygon] should be (Seq())
    }
  }
}
