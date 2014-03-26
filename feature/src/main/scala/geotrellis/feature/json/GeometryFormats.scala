package geotrellis.feature.json

import geotrellis.feature._
import spray.json._
import DefaultJsonProtocol._

trait GeometryFormats {

  /** Writes point to JsArray as [x, y] */
  private def writePoint(point: Point): JsArray =
    JsArray(JsNumber(point.x), JsNumber(point.y))

  /** JsArray of [x, y] arrays */
  private def writeLine(line: Line): JsArray =
    JsArray(line.points.map(writePoint).toList)

  /** JsArray of Lines for the polygin, first line is exterior, rest are holes*/
  private def writePolygon(polygon: Polygon): JsArray =
    JsArray(writeLine(polygon.exterior) :: polygon.holes.map(writeLine).toList)


  /** Reads Point from JsArray of [x, y] */
  private def readPoint(value: JsValue): Point = value match {
    case arr: JsArray =>
      arr.elements match {
        case Seq(JsNumber(x), JsNumber(y)) =>
          Point(x.toDouble, y.toDouble)
        case _ => throw new DeserializationException("Point [x,y] coordinates expected")
      }
    case _ => throw new DeserializationException("Point [x,y] coordinates expected")
  }

  /** Reads Line as JsArray of [x, y] point elements */
  private def readLine(value: JsValue): Line = value match {
    case arr: JsArray =>
      Line( arr.elements.map(readPoint) )
    case _ => throw new DeserializationException("Line coordinates array expected")
  }

  /** Reads Polygon from JsArray containg Lines for polygon */
  private def readPolygon(value: JsValue): Polygon = value match {
    case arr: JsArray =>
      val lines: List[Line] = arr.elements.map(readLine)
      Polygon(lines.head, lines.tail.toSet)
    case _ => throw new DeserializationException("Polygon coordinates array expected")
  }

  implicit object PointFormat extends RootJsonFormat[Point] {
    def write(p: Point) = JsObject(
      "type" -> JsString("Point"),
      "coordinates" -> JsArray(JsNumber(p.x), JsNumber(p.y))
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Point"), JsArray(Seq(JsNumber(x), JsNumber(y)))) =>
        Point(x.toDouble, y.toDouble)
      case _ => throw new DeserializationException("Point geometry expected")
    }
  }

  implicit object LineFormat extends RootJsonFormat[Line] {
    def write(line: Line) = JsObject(
      "type" -> JsString("LineString"),
      "coordinates" -> writeLine(line)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("LineString"), points) => readLine(points)
      case _ => throw new DeserializationException("LineString geometry expected")
    }
  }

  implicit object PolygonFormat extends RootJsonFormat[Polygon] {
    override def read(json: JsValue): Polygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Polygon"), linesArray) =>
        readPolygon(linesArray)
      case _ => throw new DeserializationException("Polygon geometry expected")
    }

    override def write(obj: Polygon): JsValue = JsObject(
      "type" -> JsString("Polygon"),
      "coordinates" -> writePolygon(obj)
    )
  }

  implicit object MultiPointFormat extends RootJsonFormat[MultiPoint] {
    override def read(json: JsValue): MultiPoint = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiPoint"), pointArray: JsArray) =>
        MultiPoint(pointArray.elements.map(readPoint))
      case _ => throw new DeserializationException("MultiPoint geometry expected")
    }

    override def write(obj: MultiPoint): JsValue = JsObject(
      "type" -> JsString("MultiPoint"),
      "coordinates" -> JsArray(obj.points.map(writePoint).toList)
    )
  }

  implicit object MultiLineFormat extends RootJsonFormat[MultiLine] {
    override def read(json: JsValue): MultiLine = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiLineString"), linesArray: JsArray) =>
        MultiLine(linesArray.elements.map(readLine))
      case _ => throw new DeserializationException("MultiLine geometry expected")
    }

    override def write(obj: MultiLine): JsValue = JsObject(
      "type" -> JsString("MultiLineString"),
      "coordinates" -> JsArray(obj.lines.map(writeLine).toList)
    )
  }

  implicit object MultiPolygonFormat extends RootJsonFormat[MultiPolygon] {
    override def read(json: JsValue): MultiPolygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiPolygon"), polygons: JsArray) =>
        MultiPolygon(polygons.elements.map(readPolygon))
      case _ => throw new DeserializationException("MultiPolygon geometry expected")
    }

    override def write(obj: MultiPolygon): JsValue =  JsObject(
      "type" -> JsString("MultiPolygon"),
      "coordinates" -> JsArray(obj.polygons.map(writePolygon).toList)
    )
  }

  implicit object GeometryFormat extends RootJsonFormat[Geometry] {
    def write(o: Geometry) = o match {
      case o: Point => o.toJson
      case o: Line => o.toJson
      case o: Polygon => o.toJson
      case o: MultiPoint => o.toJson
      case o: MultiLine => o.toJson
      case _ => throw new SerializationException("Unknown Geometry")
    }

    def read(value: JsValue) = value.asJsObject.getFields("type") match {
      case Seq(JsString("Point")) => value.convertTo[Point]
      case Seq(JsString("LineString")) => value.convertTo[Line]
      case Seq(JsString("Polygon")) => value.convertTo[Polygon]
      case Seq(JsString("MultiPoint")) => value.convertTo[MultiPoint]
      case Seq(JsString("MultiLineString")) => value.convertTo[MultiLine]
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
