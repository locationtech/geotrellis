package geotrellis.feature.json

import org.scalatest._

import geotrellis.feature._

import spray.json._

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._

import spray.json.DefaultJsonProtocol._

class CrsSpec extends FlatSpec with Matchers with GeoJsonSupport {
  val point = Point(6.0,1.2)
  val line = Line(Point(1,2) :: Point(1,3) :: Nil)
  val crs = NamedCRS("napkin:map:sloppy")

  def jsonBody(blob: String) =
    HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), string = blob)


  it should "should attach to a Geometry" in {
    val body =
      jsonBody(
        """{
          |  "type": "LineString",
          |  "coordinates": [[1.0, 2.0], [1.0, 3.0]],
          |  "crs": {
          |    "type": "name",
          |    "properties": {
          |      "name": "napkin:map:sloppy"
          |    }
          |  }
          |}""".stripMargin
      )
    marshal(WithCrs(line, crs)) should equal (Right(body))
    marshal(line.withCrs(crs)) should equal (Right(body))
    body.as[WithCrs[Line]] should equal (Right(WithCrs(line, crs)))
  }

  it should "should attach to a GeometryCollection" in {
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
          |  }],
          |  "crs": {
          |    "type": "name",
          |    "properties": {
          |      "name": "napkin:map:sloppy"
          |    }
          |  }
          |}""".stripMargin
      )
    val gc = GeometryCollection(List(point, line))
    marshal(WithCrs(gc, crs)) should equal (Right(body))
    body.as[WithCrs[GeometryCollection]] should equal (Right(WithCrs(gc, crs)))
  }

  it should "attach to a Feature" in {
    val f = PointFeature(Point(1, 44), "Secrets")
    val body =
    jsonBody(
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
        |}""".stripMargin
    )

    marshal(f.withCrs(crs)) should equal (Right(body))
    body.as[WithCrs[PointFeature[String]]] should equal (Right(WithCrs(f, crs)))
  }
}
