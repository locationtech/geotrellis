package geotrellis.feature.json

import org.scalatest._

import geotrellis.feature._

import spray._
import spray.json._

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._

import spray.json.DefaultJsonProtocol
import DefaultJsonProtocol._


class GeometrySpec extends FlatSpec with ShouldMatchers with GeoJsonSupport {
  val point = Point(6.0,1.2)
  val line = Line(Point(1,2) :: Point(1,3) :: Nil)

  def jsonBody(blob: String) =
    HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), string = blob)

  "Geometry" should "work point" in {
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

  it should "work line" in {
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

    //TODO: What if list: List[Geometry]
    val list: Seq[Geometry] = List(point, line)
    marshal(list) should equal (Right(body))
    body.as[Seq[Geometry]] should equal (Right(list))
  }

  it should "know about polygons" in {
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

}
