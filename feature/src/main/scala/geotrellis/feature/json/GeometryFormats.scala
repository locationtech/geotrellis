package geotrellis.feature.json

import geotrellis.feature._
import spray.json._
import DefaultJsonProtocol._

trait GeometryFormats {
  implicit object PointFormat extends RootJsonFormat[Point] {
    def write(p: Point) = JsObject(
      "type" -> JsString("Point"),
      "coordinates" -> JsArray(JsNumber(p.x), JsNumber(p.y))
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(
      JsString("Point"),
      JsArray(Seq(JsNumber(x), JsNumber(y)))
      ) => Point(x.toDouble, y.toDouble)
      case _ => throw new DeserializationException("Point geometry expected")
    }
  }


  implicit object LineFormat extends RootJsonFormat[Line] {
    def write(o: Line) = JsObject(
      "type" -> JsString("LineString"),
      "coordinates" -> JsArray(o.points.map {
        p => JsArray(JsNumber(p.x), JsNumber(p.y))
      }.toList)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("LineString"), JsArray(points)) =>
        Line(
          for (p <- points) yield
            p match {
              case JsArray(Seq(JsNumber(x), JsNumber(y))) => Point(x.toDouble, y.toDouble)
              case _ => throw new DeserializationException("LineString points exptected")
            }
        )
      case _ => throw new DeserializationException("LineString geometry expected")
    }
  }


  implicit object GeometryFormat extends RootJsonFormat[Geometry] {
    def write(o: Geometry) = o match {
      case o: Point => o.toJson
      case o: Line => o.toJson
      case _ => throw new SerializationException("Unknown Geometry")
    }

    def read(value: JsValue) = value.asJsObject.getFields("type") match {
      case Seq(JsString("Point")) => value.convertTo[Point]
      case Seq(JsString("LineString")) => value.convertTo[Line]
      case Seq(JsString(t)) => throw new DeserializationException(s"Unknown Geometry type: $t")
    }
  }

  implicit object GeometryCollection extends RootJsonFormat[Seq[Geometry]] {
    def write(list: Seq[Geometry]) = JsObject(
      "type" -> JsString("GeometryCollection"),
      "geometries" -> JsArray(list.map {
        o => o.toJson
      }.toList)
    )

    def read(value: JsValue) = value.asJsObject.getFields("geometries") match {
      case Seq(JsArray(geoms)) =>
        for (geom <- geoms) yield
          geom.convertTo[Geometry]
    }
  }
}
