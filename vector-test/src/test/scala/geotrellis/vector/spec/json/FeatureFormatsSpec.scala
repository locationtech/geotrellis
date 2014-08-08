package geotrellis.vector.json

import org.scalatest._
import geotrellis.vector._

import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._

import spray.json.DefaultJsonProtocol._

class FeatureFormatsSpec extends FlatSpec with Matchers with GeoJsonSupport {

  val pointFeature = PointFeature(Point(6.0,1.2), 123)
  val lineFeature = LineFeature(Line(Point(1,2) :: Point(1,3) :: Nil), 321)
  //val listOfFeatures: Seq[Feature[Geometry, Int]] = List(pointFeature, lineFeature)

  def jsonBody(blob: String) =
    HttpEntity(contentType = ContentType(`application/json`, `UTF-8`), string = blob)

  "Feature" should "work single point feature" in {
    val body =
      jsonBody(
        """{
          |  "type": "Feature",
          |  "geometry": {
          |    "type": "Point",
          |    "coordinates": [6.0, 1.2]
          |  },
          |  "properties": 123
          |}""".stripMargin
      )

    marshal(pointFeature) should equal (Right(body))
    body.as[PointFeature[Int]] should equal(Right(pointFeature))
  }

  it should "work single line feature" in {
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

    marshal(lineFeature) should equal (Right(body))
    body.as[LineFeature[Int]] should equal(Right(lineFeature))
  }

  it should "knows how to heterogeneous collection" in {
    val body =
     jsonBody(
       """{
         |  "type": "FeatureCollection",
         |  "features": [{
         |    "type": "Feature",
         |    "geometry": {
         |      "type": "Point",
         |      "coordinates": [6.0, 1.2]
         |    },
         |    "properties": 123
         |  }, {
         |    "type": "Feature",
         |    "geometry": {
         |      "type": "LineString",
         |      "coordinates": [[1.0, 2.0], [1.0, 3.0]]
         |    },
         |    "properties": 321
         |  }]
         |}""".stripMargin
     )

    val jsonFeatures = new JsonFeatureCollection()
    jsonFeatures += lineFeature
    jsonFeatures += pointFeature

    marshal(jsonFeatures) should equal (Right(body))

    val fc = body.as[JsonFeatureCollection].right.get
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

  case class SomeData(name: String, value: Double)
  implicit val someDataFormat = jsonFormat2(SomeData)
  it should "be able to handle Feature with custom data" in {
    val f = PointFeature(Point(1,44), SomeData("Bob", 32.2))

    val body =
      jsonBody (
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
      )

    marshal(f) should equal (Right(body))
    body.as[PointFeature[SomeData]] should equal (Right(f))
  }
}
