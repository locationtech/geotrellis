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


  /**
   * @param arr array of [x, y] point elements
   * @return Line instance representing line of points
   */
  def readLine(arr: JsArray): Line = Line(
    for (p <- arr.elements) yield p match {
      case JsArray(Seq(JsNumber(x), JsNumber(y))) => Point(x.toDouble, y.toDouble)
      case _ => throw new DeserializationException("Points [x,y] pair expected")
    }
  )

  /**
   * @return JsArray of [x, y] arrays
   */
  def writeLine(line: Line): JsArray =
    JsArray(line.points.map {p => JsArray(JsNumber(p.x), JsNumber(p.y))}.toList)
  
  implicit object LineFormat extends RootJsonFormat[Line] {
    def write(line: Line) = JsObject(
      "type" -> JsString("LineString"),
      "coordinates" -> writeLine(line)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("LineString"), points: JsArray) => readLine(points)
      case _ => throw new DeserializationException("LineString geometry expected")
    }
  }

  implicit object PolygonFormat extends RootJsonFormat[Polygon] {
    override def read(json: JsValue): Polygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Polygon"), JsArray(lineArrays)) =>
        val lines = {
          for (line <- lineArrays) yield line match {
            case l: JsArray => readLine(l)
          }
        }
        Polygon(lines.head, lines.tail.toSet)
      case _ => throw new DeserializationException("Polygon geometry expected")
    }

    override def write(obj: Polygon): JsValue = JsObject(
      "type" -> JsString("Polygon"),
      "coordinates" -> JsArray( writeLine(obj.exterior) :: obj.holes.map(writeLine).toList)
    )
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
