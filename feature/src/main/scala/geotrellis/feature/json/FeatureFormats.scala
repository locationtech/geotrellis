package geotrellis.feature.json

import geotrellis.feature._
import spray.json._
import GeometryFormats._

trait FeatureFormats {

  def writeFeatureJson[D: JsonWriter](obj: Feature[D]): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(obj.geom),
      "properties" -> obj.data.toJson
    )
  }

  def readFeatureJson[D: JsonReader, G <: Geometry: JsonReader, F <: Feature[D]](value: JsValue)(create : (G, D) => F): F = {
    value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString("Feature"), geom, data) =>
        val g = geom.convertTo[G]
        val d = data.convertTo[D]
        create(g,d)
      case _ => throw new DeserializationException("Feature expected")
    }
  }


  implicit def featureFormat[D: JsonFormat] = new RootJsonFormat[Feature[D]] {
    override def read(json: JsValue): Feature[D] =
      readFeatureJson[D, Geometry, Feature[D]](json){ case (geom, d) => geom match {
        case g: Point => PointFeature(g, d)
        case g: Line => LineFeature(g, d)
        case g: Polygon => PolygonFeature(g, d)
        case g: MultiPoint => MultiPointFeature(g, d)
        case g: MultiLine => MultiLineFeature(g, d)
        case g: MultiPolygon => MultiPolygonFeature(g, d)
        case g: GeometryCollection => GeometryCollectionFeature(g, d)
      }}

    override def write(obj: Feature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def pointFeatureFormat[D: JsonFormat] = new RootJsonFormat[PointFeature[D]] {
    override def read(json: JsValue): PointFeature[D] =
      readFeatureJson[D, Point, PointFeature[D]](json){PointFeature.apply}
    override def write(obj: PointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def lineFeatureFormat[D: JsonFormat] = new RootJsonFormat[LineFeature[D]] {
    override def read(json: JsValue): LineFeature[D] =
      readFeatureJson[D, Line, LineFeature[D]](json){LineFeature.apply}
    override def write(obj: LineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def polygonFeatureFormat[D: JsonFormat] = new RootJsonFormat[PolygonFeature[D]] {
    override def read(json: JsValue): PolygonFeature[D] =
      readFeatureJson[D, Polygon, PolygonFeature[D]](json){PolygonFeature.apply}
    override def write(obj: PolygonFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiPointFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiPointFeature[D]] {
    override def read(json: JsValue): MultiPointFeature[D] =
      readFeatureJson[D, MultiPoint, MultiPointFeature[D]](json){MultiPointFeature.apply}
    override def write(obj: MultiPointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiLineFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiLineFeature[D]] {
    override def read(json: JsValue): MultiLineFeature[D] =
      readFeatureJson[D, MultiLine, MultiLineFeature[D]](json){MultiLineFeature.apply}
    override def write(obj: MultiLineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiPolygonFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiPolygonFeature[D]] {
    override def read(json: JsValue): MultiPolygonFeature[D] =
      readFeatureJson[D, MultiPolygon, MultiPolygonFeature[D]](json){MultiPolygonFeature.apply}
    override def write(obj: MultiPolygonFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit object featureCollectionFormat extends RootJsonFormat[JsonFeatureCollection] {
    override def read(json: JsValue): JsonFeatureCollection = json.asJsObject.getFields("type", "features") match {
      case Seq(JsString("FeatureCollection"), JsArray(features)) => new JsonFeatureCollection(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollection): JsValue = obj.toJson
  }
}

object FeatureFormats extends FeatureFormats