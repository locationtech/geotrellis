package geotrellis.feature.json

import geotrellis.feature._
import spray.json._


trait FeatureFormats extends GeometryFormats {

  class PointFeatureFormat[D: JsonFormat] extends RootJsonFormat[PointFeature[D]] {
    def write(f: PointFeature[D]) =
      JsObject(
        "type" -> JsString("Feature"),
        "geometry" -> f.geom.toJson,
        "properties" -> f.data.toJson
      )

    def read(value: JsValue) = value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString(fType), geom, data) =>
        PointFeature(geom.convertTo[Point], data.convertTo[D])
      case _ => throw new DeserializationException("Point feature expected")
    }
  }
  implicit def pointFeatureFormat[D: JsonFormat]: RootJsonFormat[PointFeature[D]] =
    new PointFeatureFormat[D]


    class LineFeatureFormat[D: JsonFormat] extends RootJsonFormat[LineFeature[D]] {
    def write(f: LineFeature[D]) =
      JsObject(
        "type" -> JsString("Feature"),
        "geometry" -> f.geom.toJson,
        "properties" -> f.data.toJson
      )

    def read(value: JsValue) = value.asJsObject.getFields("type", "geometry", "properties") match {
      case Seq(JsString(fType), geom, data) =>
        LineFeature(LineFormat.read(geom), data.convertTo[D])
      case _ => throw new DeserializationException("Point feature expected")
    }
  }
  implicit def lineFeatureFormat[D: JsonFormat]: RootJsonFormat[LineFeature[D]] =
    new LineFeatureFormat[D]

  class FeatureFormat[D: JsonFormat] extends RootJsonFormat[Feature[Geometry,D]] {
    def write(o: Feature[Geometry, D]) = o match {
      case pf: PointFeature[D] => pointFeatureFormat[D].write(pf)
      case lf: LineFeature[D] => lineFeatureFormat[D].write(lf)
      case _ => throw new SerializationException("Unknown Feature")
    }

    def read(value: JsValue) = value.asJsObject.getFields("geometry").head.asJsObject.getFields("type") match {
      //I feel bad about having to upcast here
      case Seq(JsString("Point")) => pointFeatureFormat[D].read(value).asInstanceOf[Feature[Geometry, D]]
      case Seq(JsString("LineString")) => lineFeatureFormat[D].read(value).asInstanceOf[Feature[Geometry, D]]
      case Seq(JsString(t)) => throw new DeserializationException(s"Unknown Geometry type: $t")
    }
  }
  implicit def featureFormat[D: JsonFormat]: RootJsonFormat[Feature[Geometry,D]] =
    new FeatureFormat[D]


  class FeatureCollection[D: JsonFormat] extends RootJsonFormat[Seq[Feature[Geometry, D]]] {
    def write(list: Seq[Feature[Geometry, D]]) = JsObject(
      "type" -> JsString("FeatureCollection"),
      "features" -> JsArray(list.map {
        f => featureFormat[D].write(f)
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
