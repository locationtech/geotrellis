package geotrellis.feature.json

import geotrellis.feature._
import spray.json._

object FeatureFormats {
  import GeometryFormats._

  def writeFeatureJson[D: JsonWriter](obj: Feature[D]): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(obj.geom),
      "properties" -> obj.data.toJson
    )
  }

  def readFeatureJson[D: JsonReader](value: JsValue): Feature[D] = {
    value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString("Feature"), geom, data) =>
        val g = GeometryFormat.read(geom) //have to use generic generic reader since result type is unknown at compile.
        val d = data.convertTo[D]

        g match {
          case g: Point => PointFeature(g, d)
          case g: Line => LineFeature(g, d)
          case _ => ???
        }
      case _ => throw new DeserializationException("Feature expected")
    }
  }

  implicit def pointFeatureFormat[D: JsonFormat] = new RootJsonFormat[PointFeature[D]] {
    override def read(json: JsValue): PointFeature[D] = readFeatureJson[D](json).asInstanceOf[PointFeature[D]]
    override def write(obj: PointFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit def lineFeatureFormat[D: JsonFormat] = new RootJsonFormat[LineFeature[D]] {
    override def read(json: JsValue): LineFeature[D] = readFeatureJson[D](json).asInstanceOf[LineFeature[D]]
    override def write(obj: LineFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit def polygonFeatureFormat[D: JsonFormat] = new RootJsonFormat[PolygonFeature[D]] {
    override def read(json: JsValue): PolygonFeature[D] = readFeatureJson[D](json).asInstanceOf[PolygonFeature[D]]
    override def write(obj: PolygonFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit def multiPointFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiPointFeature[D]] {
    override def read(json: JsValue): MultiPointFeature[D] = readFeatureJson[D](json).asInstanceOf[MultiPointFeature[D]]
    override def write(obj: MultiPointFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit def multiLineFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiLineFeature[D]] {
    override def read(json: JsValue): MultiLineFeature[D] = readFeatureJson[D](json).asInstanceOf[MultiLineFeature[D]]
    override def write(obj: MultiLineFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit def multiPolygonFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiPolygonFeature[D]] {
    override def read(json: JsValue): MultiPolygonFeature[D] = readFeatureJson[D](json).asInstanceOf[MultiPolygonFeature[D]]
    override def write(obj: MultiPolygonFeature[D]): JsValue = writeFeatureJson(obj)
  }

  implicit object featureCollectionFormat extends RootJsonFormat[JsonFeatureCollection] {
    override def read(json: JsValue): JsonFeatureCollection = json.asJsObject.getFields("type", "features") match {
      case Seq(JsString("FeatureCollection"), JsArray(features)) => new JsonFeatureCollection(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollection): JsValue = obj.toJson
  }
}