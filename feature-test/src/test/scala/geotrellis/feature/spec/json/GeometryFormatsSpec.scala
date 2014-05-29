package geotrellis.feature.json

import org.scalatest._

import geotrellis.feature._

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._

class GeometryFormatsSpec extends FlatSpec with Matchers with GeoJsonSupport {

  val point = Point(6.0,1.2)
  val line = Line(Point(1,2) :: Point(1,3) :: Nil)

  def jsonBody(blob: String) =
    HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), string = blob)

  "GeometryFormats" should "know about Points" in {
    val body =
      jsonBody(
        """{
          |  "type": "Point",
          |  "coordinates": [6.0, 1.2]
          |}""".stripMargin
      )

    marshal(point) should equal (Right(body))
    body.as[Point] should equal(Right(point))
  }

  it should "know about MultiPoints" in {
    val mp =
      MultiPoint(List(Point(0,0), Point(0,1)))
    val body =
      jsonBody(
        """{
          |  "type": "MultiPoint",
          |  "coordinates": [[0.0, 0.0], [0.0, 1.0]]
          |}""".stripMargin
      )

    marshal(mp) should equal (Right(body))
    body.as[MultiPoint] should equal(Right(mp))
  }

  it should "know about Lines" in {
    val body =
      jsonBody(
        """{
          |  "type": "LineString",
          |  "coordinates": [[1.0, 2.0], [1.0, 3.0]]
          |}""".stripMargin
      )

    marshal(line) should equal (Right(body))
    body.as[Line] should equal(Right(line))
  }

  it should "know about MultiLines" in {
    val ml =
      MultiLine(Line(Point(0,0), Point(0,1)) :: Line(Point(1,0), Point(1,1)) :: Nil)
    val body =
      jsonBody(
        """{
          |  "type": "MultiLineString",
          |  "coordinates": [[[0.0, 0.0], [0.0, 1.0]], [[1.0, 0.0], [1.0, 1.0]]]
          |}""".stripMargin
      )

    marshal(ml) should equal (Right(body))
    body.as[MultiLine] should equal(Right(ml))
  }

  it should "know about Polygons" in {
    val polygon =
      Polygon(
        Line(Point(0,0), Point(0,1), Point(1,1), Point(0,0))
      )
    val body =
      jsonBody(
        """{
          |  "type": "Polygon",
          |  "coordinates": [[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]]
          |}""".stripMargin
      )

    marshal(polygon) should equal (Right(body))
    body.as[Polygon] should equal(Right(polygon))
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
      jsonBody(
        """{
          |  "type": "MultiPolygon",
          |  "coordinates": [[[[0.0, 0.0], [0.0, 1.0], [1.0, 1.0], [0.0, 0.0]]], [[[1.0, 1.0], [1.0, 2.0], [2.0, 2.0], [1.0, 1.0]]]]
          |}""".stripMargin
      )

    marshal(mp) should equal (Right(body))
    body.as[MultiPolygon] should equal(Right(mp))
  }


  it should "know how to collection" in {
    val body =
      jsonBody(
        """{
          |  "type": "GeometryCollection",
          |  "geometries": [{
          |    "type": "Point",
          |    "coordinates": [6.0, 1.2]
          |  }, {
          |    "type": "LineString",
          |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
          |  }]
          |}""".stripMargin
      )

    //What if list: List[Geometry], won't work, JsonFormat is invariant on it's type-parameter
    //To get that functionality you would need a separate marshaller that is contravariant (that way danger lies)
    val gc: GeometryCollection = GeometryCollection(List(point, line))
    marshal(gc) should equal (Right(body))
    body.as[GeometryCollection] should equal (Right(gc))
  }

  it should "read a Feature as if it was a Geometry" in {
    val line = Line(Point(1,2) :: Point(1,3) :: Nil);
    val body =
    jsonBody(
      """{
        |  "type": "Feature",
        |  "geometry": {
        |    "type": "LineString",
        |    "coordinates": [[1.0, 2.0], [1.0, 3.0]]
        |  },
        |  "properties": 321
        |}""".stripMargin
    )
    //I test it only for a Line, but the logic works for all Geomtries
    body.as[Line] should equal(Right(line))  
  }
}
