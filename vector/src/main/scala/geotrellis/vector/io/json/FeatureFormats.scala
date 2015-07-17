package geotrellis.vector.io.json

import geotrellis.vector._
import spray.json._
import GeometryFormats._

trait FeatureFormats {

  /** Serializes a geojson feature object
    *
    * @param A Feature object
    * @tparam The type (which must have an implicit method to resolve the transformation from json)
    * @return The GeoJson compliant spray.JsValue
    */
  def writeFeatureJson[D: JsonWriter](obj: Feature[D]): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(obj.geom),
      "properties" -> obj.data.toJson
    )
  }

  def writeFeatureJsonWithID[D: JsonWriter](idFeature: (String, Feature[D])): JsValue = {
    JsObject(
      "type" -> JsString("Feature"),
      "geometry" -> GeometryFormat.write(idFeature._2.geom),
      "properties" -> idFeature._2.data.toJson,
      "id" -> JsString(idFeature._1)
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

  def readFeatureJsonWithID[D: JsonReader, G <: Geometry: JsonReader, F <: Feature[D]](value: JsValue)(create : (G, D, String) => (String, F)): (String, F) = {
    value.asJsObject.getFields("type", "geometry", "properties", "id") match {
      case Seq(JsString("Feature"), geom, data, id) =>
        val g = geom.convertTo[G]
        val d = data.convertTo[D]
        val i = id.toString
        create(g, d, i)
      case _ => throw new DeserializationException("Feature expected")
    }
  }

  implicit def featureFormat[D: JsonFormat] = new RootJsonFormat[Feature[D]] {
    override def read(json: JsValue): Feature[D] =
      readFeatureJson[D, Geometry, Feature[D]](json) {
        case (geom, d) => geom match {
          case g: Point => PointFeature(g, d)
          case g: Line => LineFeature(g, d)
          case g: Polygon => PolygonFeature(g, d)
          case g: MultiPoint => MultiPointFeature(g, d)
          case g: MultiLine => MultiLineFeature(g, d)
          case g: MultiPolygon => MultiPolygonFeature(g, d)
          case g: GeometryCollection => GeometryCollectionFeature(g, d)
        }
      }

    override def write(obj: Feature[D]): JsValue =
      writeFeatureJson(obj)
  }


  implicit def pointFeatureReader[D: JsonReader] = new RootJsonReader[PointFeature[D]] {
    override def read(json: JsValue): PointFeature[D] =
      readFeatureJson[D, Point, PointFeature[D]](json){PointFeature.apply}
  }

  implicit def pointFeatureWriter[D: JsonWriter] = new RootJsonWriter[PointFeature[D]] {
    override def write(obj: PointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def pointFeatureFormat[D: JsonFormat] = new RootJsonFormat[PointFeature[D]] {
    override def read(json: JsValue): PointFeature[D] =
      readFeatureJson[D, Point, PointFeature[D]](json){PointFeature.apply}

    override def write(obj: PointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def lineFeatureReader[D: JsonReader] = new RootJsonReader[LineFeature[D]] {
    override def read(json: JsValue): LineFeature[D] =
      readFeatureJson[D, Line, LineFeature[D]](json){LineFeature.apply}
  }

  implicit def lineFeatureWriter[D: JsonWriter] = new RootJsonWriter[LineFeature[D]] {
    override def write(obj: LineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def lineFeatureFormat[D: JsonFormat] = new RootJsonFormat[LineFeature[D]] {
    override def read(json: JsValue): LineFeature[D] =
      readFeatureJson[D, Line, LineFeature[D]](json){LineFeature.apply}

    override def write(obj: LineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def polygonFeatureReader[D: JsonReader] = new RootJsonReader[PolygonFeature[D]] {
    override def read(json: JsValue): PolygonFeature[D] =
      readFeatureJson[D, Polygon, PolygonFeature[D]](json){PolygonFeature.apply}
  }

  implicit def polygonFeatureWriter[D: JsonWriter] = new RootJsonWriter[PolygonFeature[D]] {
    override def write(obj: PolygonFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def polygonFeatureFormat[D: JsonFormat] = new RootJsonFormat[PolygonFeature[D]] {
    override def read(json: JsValue): PolygonFeature[D] =
      readFeatureJson[D, Polygon, PolygonFeature[D]](json){PolygonFeature.apply}

    override def write(obj: PolygonFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiPointFeatureReader[D: JsonReader] = new RootJsonReader[MultiPointFeature[D]] {
    override def read(json: JsValue): MultiPointFeature[D] =
      readFeatureJson[D, MultiPoint, MultiPointFeature[D]](json){MultiPointFeature.apply}
  }

  implicit def multiPointFeatureWriter[D: JsonWriter] = new RootJsonWriter[MultiPointFeature[D]] {
    override def write(obj: MultiPointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiPointFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiPointFeature[D]] {
    override def read(json: JsValue): MultiPointFeature[D] =
      readFeatureJson[D, MultiPoint, MultiPointFeature[D]](json){MultiPointFeature.apply}

    override def write(obj: MultiPointFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiLineFeatureReader[D: JsonReader] = new RootJsonReader[MultiLineFeature[D]] {
    override def read(json: JsValue): MultiLineFeature[D] =
      readFeatureJson[D, MultiLine, MultiLineFeature[D]](json){MultiLineFeature.apply}
  }

  implicit def multiLineFeatureWriter[D: JsonWriter] = new RootJsonWriter[MultiLineFeature[D]] {
    override def write(obj: MultiLineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiLineFeatureFormat[D: JsonFormat] = new RootJsonFormat[MultiLineFeature[D]] {
    override def read(json: JsValue): MultiLineFeature[D] =
      readFeatureJson[D, MultiLine, MultiLineFeature[D]](json){MultiLineFeature.apply}

    override def write(obj: MultiLineFeature[D]): JsValue =
      writeFeatureJson(obj)
  }

  implicit def multiPolygonFeatureReader[D: JsonReader] = new RootJsonReader[MultiPolygonFeature[D]] {
    override def read(json: JsValue): MultiPolygonFeature[D] =
      readFeatureJson[D, MultiPolygon, MultiPolygonFeature[D]](json){MultiPolygonFeature.apply}
  }

  implicit def multiPolygonFeatureWriter[D: JsonWriter] = new RootJsonWriter[MultiPolygonFeature[D]] {
    override def write(obj: MultiPolygonFeature[D]): JsValue =
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
      case Seq(JsString("FeatureCollection"), JsArray(features)) => JsonFeatureCollection(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollection): JsValue = obj.toJson
  }

  implicit object featureCollectionMapFormat extends RootJsonFormat[JsonFeatureCollectionMap] {
    override def read(json: JsValue): JsonFeatureCollectionMap = json.asJsObject.getFields("type", "features") match {
      case Seq(JsString("FeatureCollection"), JsArray(features)) => JsonFeatureCollectionMap(features)
      case _ => throw new DeserializationException("FeatureCollection expected")
    }
    override def write(obj: JsonFeatureCollectionMap): JsValue = obj.toJson
  }
}

object FeatureFormats extends FeatureFormats
