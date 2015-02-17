package geotrellis.vector.io.json

import geotrellis.vector._
import spray.json._

/**
 * Implements Spray JsonFormats for Geometry objects.
 * Import or extend this object directly to use them with default spray-json (un)marshaller
 */
trait GeometryFormats {
  /** Writes point to JsArray as [x, y] */
  private def writePointCoords(point: Point): JsArray =
    JsArray(JsNumber(point.x), JsNumber(point.y))

  /** JsArray of [x, y] arrays */
  private def writeLineCoords(line: Line): JsArray =
    JsArray(line.points.map(writePointCoords).toList)

  /** JsArray of Lines for the polygin, first line is exterior, rest are holes*/
  private def writePolygonCoords(polygon: Polygon): JsArray =
    JsArray(writeLineCoords(polygon.exterior) :: polygon.holes.map(writeLineCoords).toList)


  /** Reads Point from JsArray of [x, y] */
  private def readPointCoords(value: JsValue): Point = value match {
    case arr: JsArray =>
      arr.elements match {
        case Seq(JsNumber(x), JsNumber(y)) =>
          Point(x.toDouble, y.toDouble)
        case _ => throw new DeserializationException("Point [x,y] coordinates expected")
      }
    case _ => throw new DeserializationException("Point [x,y] coordinates expected")
  }

  /** Reads Line as JsArray of [x, y] point elements */
  private def readLineCoords(value: JsValue): Line = value match {
    case arr: JsArray =>
      Line( arr.elements.map(readPointCoords) )
    case _ => throw new DeserializationException("Line coordinates array expected")
  }

  /** Reads Polygon from JsArray containg Lines for polygon */
  private def readPolygonCoords(value: JsValue): Polygon = value match {
    case arr: JsArray =>
      val lines: List[Line] = 
        arr.elements
           .map(readLineCoords)
           .map(_.closed)

      Polygon(lines.head, lines.tail.toSet)
    case _ => throw new DeserializationException("Polygon coordinates array expected")
  }

  implicit object PointFormat extends RootJsonFormat[Point] {
    def write(p: Point) = JsObject(
      "type" -> JsString("Point"),
      "coordinates" -> JsArray(JsNumber(p.x), JsNumber(p.y))
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Point"), point) =>      
        readPointCoords(point)
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(value))
      case _ => throw new DeserializationException("Point geometry expected")
    }
  }

  implicit object LineFormat extends RootJsonFormat[Line] {
    def write(line: Line) = JsObject(
      "type" -> JsString("LineString"),
      "coordinates" -> writeLineCoords(line)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("LineString"), points) =>
        readLineCoords(points)
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(value))        
      case _ => throw new DeserializationException("LineString geometry expected")
    }
  }

  implicit object PolygonFormat extends RootJsonFormat[Polygon] {
    override def read(json: JsValue): Polygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Polygon"), linesArray) =>
        readPolygonCoords(linesArray)
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(json))
      case _ => throw new DeserializationException("Polygon geometry expected")
    }

    override def write(obj: Polygon): JsValue = JsObject(
      "type" -> JsString("Polygon"),
      "coordinates" -> writePolygonCoords(obj)
    )
  }

  implicit object ExtentFormat extends RootJsonFormat[Extent] {
    def write(extent: Extent) = 
      JsObject(
        "xmin" -> JsNumber(extent.xmin),
        "ymin" -> JsNumber(extent.ymin),
        "xmax" -> JsNumber(extent.xmax),
        "ymax" -> JsNumber(extent.ymax)
      )

    def read(value: JsValue): Extent =
      value.asJsObject.getFields("xmin", "ymin", "xmax", "ymax") match {
        case Seq(JsNumber(xmin), JsNumber(ymin), JsNumber(xmax), JsNumber(ymax)) =>
          Extent(xmin.toDouble, ymin.toDouble, xmax.toDouble, ymax.toDouble)
        case _ =>
          throw new DeserializationException("Extent [xmin,ymin,xmax,ymax] expected")
      }
  }

  implicit object MultiPointFormat extends RootJsonFormat[MultiPoint] {
    override def read(json: JsValue): MultiPoint = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiPoint"), pointArray: JsArray) =>
        MultiPoint(pointArray.elements.map(readPointCoords))
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(json))
      case _ => throw new DeserializationException("MultiPoint geometry expected")
    }

    override def write(obj: MultiPoint): JsValue = JsObject(
      "type" -> JsString("MultiPoint"),
      "coordinates" -> JsArray(obj.points.map(writePointCoords).toList)
    )
  }

  implicit object MultiLineFormat extends RootJsonFormat[MultiLine] {
    override def read(json: JsValue): MultiLine = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiLineString"), linesArray: JsArray) =>
        MultiLine(linesArray.elements.map(readLineCoords))
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(json))
      case _ => throw new DeserializationException("MultiLine geometry expected")
    }

    override def write(obj: MultiLine): JsValue = JsObject(
      "type" -> JsString("MultiLineString"),
      "coordinates" -> JsArray(obj.lines.map(writeLineCoords).toList)
    )
  }

  implicit object MultiPolygonFormat extends RootJsonFormat[MultiPolygon] {
    override def read(json: JsValue): MultiPolygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("MultiPolygon"), polygons: JsArray) =>
        MultiPolygon(polygons.elements.map(readPolygonCoords))
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(json))
      case _ => throw new DeserializationException("MultiPolygon geometry expected")
    }

    override def write(obj: MultiPolygon): JsValue =  JsObject(
      "type" -> JsString("MultiPolygon"),
      "coordinates" -> JsArray(obj.polygons.map(writePolygonCoords).toList)
    )
  }

  implicit object GeometryCollectionFormat extends RootJsonFormat[GeometryCollection] {
    def write(gc: GeometryCollection) = JsObject(
      "type" -> JsString("GeometryCollection"),
      "geometries" -> JsArray(
        List(
          gc.points.map(_.toJson).toList,
          gc.lines.map(_.toJson).toList,
          gc.polygons.map(_.toJson).toList,
          gc.multiPoints.map(_.toJson).toList,
          gc.multiLines.map(_.toJson).toList,
          gc.multiPolygons.map(_.toJson).toList,
          gc.geometryCollections.map(_.toJson).toList
        ).flatten
      )
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "geometries") match {
      case Seq(JsString("GeometryCollection"), JsArray(geomsJson)) =>
        GeometryCollection(geomsJson.map(g => GeometryFormat.read(g)))
      case Seq(JsString("Feature")) => 
        read(unwrapFeature(value))
      case _ => throw new DeserializationException("GeometryCollection expected")
    }
  }

  implicit object GeometryFormat extends RootJsonFormat[Geometry] {
    def write(o: Geometry) = o match {
      case o: Point => o.toJson
      case o: Line => o.toJson
      case o: Polygon => o.toJson
      case o: MultiPolygon => o.toJson
      case o: MultiPoint => o.toJson
      case o: MultiLine => o.toJson
      case o: GeometryCollection => o.toJson
      case _ => throw new SerializationException("Unknown Geometry")
    }

    def read(value: JsValue) = value.asJsObject.getFields("type") match {
      case Seq(JsString("Feature")) => read(unwrapFeature(value))
      case Seq(JsString("Point")) => value.convertTo[Point]
      case Seq(JsString("LineString")) => value.convertTo[Line]
      case Seq(JsString("Polygon")) => value.convertTo[Polygon]
      case Seq(JsString("MultiPolygon")) => value.convertTo[MultiPolygon]
      case Seq(JsString("MultiPoint")) => value.convertTo[MultiPoint]
      case Seq(JsString("MultiLineString")) => value.convertTo[MultiLine]
      case Seq(JsString("GeometryCollection")) => value.convertTo[GeometryCollection]      
      case Seq(JsString(t)) => throw new DeserializationException(s"Unknown Geometry type: $t")
    }
  }

  /**
   * If given JSON is actually a Feature we extract 'geometry' field and discard the rest,
   * in all other cases the value is returned unaltered.
   * Thus we can comperehand any Feature as a Geometry if we don't care about 'properties'.
   */
  private def unwrapFeature(value: JsValue): JsValue = {
    value.asJsObject.getFields("type", "geometry") match {
      case Seq(JsString("Feature"), geom) =>  geom
      case _ => value
    }
  }
}

object GeometryFormats extends GeometryFormats
