package geotrellis.feature.json

import geotrellis.feature._
import spray.json._


trait FeatureFormats extends GeometryFormats {
  private def writeFeature[G <: Geometry: JsonWriter, D: JsonWriter](feature: Feature[G, D]) =
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> feature.geom.toJson,
      "properties" -> feature.data.toJson
    )

  private def readFeature[G <: Geometry: JsonReader, D: JsonReader](value: JsValue): Feature[G, D] = {
    value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString("Feature"), geom, data) =>
        val g = geom.convertTo[G]
        val d = data.convertTo[D]

        g match {
          case g:Point => PointFeature(g, d).asInstanceOf[Feature[G, D]]
          case g:Line => LineFeature(g, d).asInstanceOf[Feature[G, D]]
        }

      case _ => throw new DeserializationException("Feature expected")
    }
  }


  /*
   * Each one of these is needed per type because JsonFormat is not covariant with T
   * There is no other JsonFormat that will be ever good enough for these people
   *
   * If there is an easier way than all this copy/paste, I would like to find it
   */
  class PointFeatureFormat[D: JsonFormat] extends RootJsonFormat[PointFeature[D]] {
    def write(f: PointFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[Point, D](value).asInstanceOf[PointFeature[D]]
  }
  implicit def PointFeatureFormat[D: JsonFormat]: RootJsonFormat[PointFeature[D]] =
    new PointFeatureFormat[D]

  class MultiPointFeatureFormat[D: JsonFormat] extends RootJsonFormat[MultiPointFeature[D]] {
    def write(f: MultiPointFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[MultiPoint, D](value).asInstanceOf[MultiPointFeature[D]]
  }
  implicit def MultiPointFeatureFormat[D: JsonFormat]: RootJsonFormat[MultiPointFeature[D]] =
    new MultiPointFeatureFormat[D]

  class LineFeatureFormat[D: JsonFormat] extends RootJsonFormat[LineFeature[D]] {
    def write(f: LineFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[Line, D](value).asInstanceOf[LineFeature[D]]
  }
  implicit def lineFeatureFormat[D: JsonFormat]: RootJsonFormat[LineFeature[D]] =
    new LineFeatureFormat[D]

  class MultiLineFeatureFormat[D: JsonFormat] extends RootJsonFormat[MultiLineFeature[D]] {
    def write(f: MultiLineFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[MultiLine, D](value).asInstanceOf[MultiLineFeature[D]]
  }
  implicit def MultiLineFeatureFormat[D: JsonFormat]: RootJsonFormat[MultiLineFeature[D]] =
    new MultiLineFeatureFormat[D]

  class PolygonFeatureFormat[D: JsonFormat] extends RootJsonFormat[PolygonFeature[D]] {
    def write(f: PolygonFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[Polygon, D](value).asInstanceOf[PolygonFeature[D]]
  }
  implicit def PolygonFeatureFormat[D: JsonFormat]: RootJsonFormat[PolygonFeature[D]] =
    new PolygonFeatureFormat[D]

  class MultiPolygonFeatureFormat[D: JsonFormat] extends RootJsonFormat[MultiPolygonFeature[D]] {
    def write(f: MultiPolygonFeature[D]) = writeFeature(f)
    def read(value: JsValue) = readFeature[MultiPolygon, D](value).asInstanceOf[MultiPolygonFeature[D]]
  }
  implicit def MultiPolygonFeatureFormat[D: JsonFormat]: RootJsonFormat[MultiPolygonFeature[D]] =
    new MultiPolygonFeatureFormat[D]


  class FeatureFormat[G <:Geometry :JsonFormat, D: JsonFormat] extends RootJsonFormat[Feature[G,D]] {
    def write(o: Feature[G, D]) = writeFeature(o)
    def read(value: JsValue) = readFeature[G, D](value)
  }
  implicit def featureFormat[G <:Geometry :JsonFormat, D: JsonFormat]: RootJsonFormat[Feature[G,D]] =
    new FeatureFormat[G, D]


  class FeatureCollection[D: JsonFormat] extends RootJsonFormat[Seq[Feature[Geometry, D]]] {
    def write(list: Seq[Feature[Geometry, D]]) = JsObject(
      "type" -> JsString("FeatureCollection"),
      "features" -> JsArray(list.map {
        f => featureFormat[Geometry, D].write(f)
      }.toList)
    )
    def read(value: JsValue) = value.asJsObject.getFields("features") match {
      case Seq(JsArray(features)) =>
        for (feature <- features) yield
          feature.convertTo[Feature[Geometry, D]]
    }
  }
  implicit def featureCollection[D: JsonFormat] =
    new FeatureCollection[D]
}
