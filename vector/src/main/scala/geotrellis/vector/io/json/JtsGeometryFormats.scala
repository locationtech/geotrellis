/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector.io.json

import geotrellis.vector._
import geotrellis.vector.GeomFactory.factory

import com.vividsolutions.jts.{geom => jts}
import spray.json._

import scala.collection.JavaConverters._
import scala.collection.mutable

/** A trait that implements Spray JsonFormats for JTS's Geometry objects.
  * This is useful if you want to work the Z value of JTS geometries.
  * @note Import or extend this object directly to use them with default spray-json (un)marshaller
  */
trait JtsGeometryFormats {
  /** Writes point to JsArray as [x, y] */
  private def writeJtsPointCoords(coord: jts.Coordinate): JsArray =
    if(java.lang.Double.isNaN(coord.z)) {
      JsArray(JsNumber(coord.x), JsNumber(coord.y))
    } else {
      JsArray(JsNumber(coord.x), JsNumber(coord.y), JsNumber(coord.z))
    }

  /** Writes point to JsArray as [x, y] */
  private def writeJtsPointCoords(point: jts.Point): JsArray =
    writeJtsPointCoords(point.getCoordinate)

  /** JsArray of [x, y] arrays */
  private def writeJtsLineCoords(line: jts.LineString): JsArray =
    JsArray(line.getCoordinates.map(writeJtsPointCoords).toVector)

  /** JsArray of Lines for the polygin, first line is exterior, rest are holes*/
  private def writeJtsPolygonCoords(polygon: jts.Polygon): JsArray = {
    val rings = mutable.ListBuffer[jts.LineString]()
    rings += polygon.getExteriorRing.asInstanceOf[jts.LineString]
    for(i <- 0 until polygon.getNumInteriorRing) {
      rings += polygon.getInteriorRingN(i)
    }
    JsArray(rings.map { l => writeJtsLineCoords(l) }.toVector)
  }

  /** Reads Point from JsArray of [x, y] */
  private def readJtsPointCoords(value: JsValue): jts.Coordinate = value match {
    case arr: JsArray =>
      val coord =
        arr.elements match {
          case Seq(JsNumber(x), JsNumber(y)) =>
            new jts.Coordinate(x.toDouble, y.toDouble)
          case Seq(JsNumber(x), JsNumber(y), JsNumber(z)) =>
            new jts.Coordinate(x.toDouble, y.toDouble, z.toDouble)
          case _ => throw new DeserializationException("Point [x,y] or [x,y,z] coordinates expected")
        }
      coord
    case _ => throw new DeserializationException("Point [x,y] coordinates expected")
  }

  /** Reads Point from JsArray of [x, y] */
  private def readJtsPoint(value: JsValue): jts.Point =
    factory.createPoint(readJtsPointCoords(value))

  /** Reads Line as JsArray of [x, y] point elements */
  private def readJtsLineCoords(value: JsValue): jts.LineString = value match {
    case arr: JsArray =>
      factory.createLineString(arr.elements.map(readJtsPointCoords).toArray)
    case _ => throw new DeserializationException("LineString coordinates array expected")
  }

  /** Reads Polygon from JsArray containg Lines for polygon */
  private def readJtsPolygonCoords(value: JsValue): jts.Polygon = value match {
    case arr: JsArray =>
      val lines: Vector[jts.LinearRing] =
        arr.elements
          .map(readJtsLineCoords)
          .map { ls =>
            val coords = ls.getCoordinates
            if(ls.isClosed) factory.createLinearRing(coords)
            else {
              factory.createLinearRing(coords :+ coords.head)
            }
          }

      factory.createPolygon(lines.head, lines.tail.toArray)
    case _ => throw new DeserializationException("Polygon coordinates array expected")
  }

  implicit object JtsPointFormat extends RootJsonFormat[jts.Point] {
    def write(p: jts.Point) = JsObject(
      "type" -> JsString("Point"),
      "coordinates" -> writeJtsPointCoords(p)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Point"), point) =>
        readJtsPoint(point)
      case Seq(JsString("Feature")) =>
        read(unwrapFeature(value))
      case _ => throw new DeserializationException("Point geometry expected")
    }
  }

  implicit object JtsLineFormat extends RootJsonFormat[jts.LineString] {
    def write(line: jts.LineString) = JsObject(
      "type" -> JsString("LineString"),
      "coordinates" -> writeJtsLineCoords(line)
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("LineString"), points) =>
        readJtsLineCoords(points)
      case Seq(JsString("Feature")) =>
        read(unwrapFeature(value))
      case _ => throw new DeserializationException("LineString geometry expected")
    }
  }

  implicit object JtsPolygonFormat extends RootJsonFormat[jts.Polygon] {
    override def read(json: JsValue): jts.Polygon = json.asJsObject.getFields("type", "coordinates") match {
      case Seq(JsString("Polygon"), linesArray) =>
        readJtsPolygonCoords(linesArray)
      case Seq(JsString("Feature")) =>
        read(unwrapFeature(json))
      case _ => throw new DeserializationException("Polygon geometry expected")
    }

    override def write(obj: jts.Polygon): JsValue = JsObject(
      "type" -> JsString("Polygon"),
      "coordinates" -> writeJtsPolygonCoords(obj)
    )
  }

  implicit object JtsMultiPointFormat extends RootJsonFormat[jts.MultiPoint] {
    override def read(json: JsValue): jts.MultiPoint =
      json.asJsObject.getFields("type", "coordinates") match {
        case Seq(JsString("MultiPoint"), pointArray: JsArray) =>
          factory.createMultiPoint(pointArray.elements.map(readJtsPoint).toArray)
        case Seq(JsString("Feature")) =>
          read(unwrapFeature(json))
        case _ => throw new DeserializationException("MultiPoint geometry expected")
      }

    override def write(obj: jts.MultiPoint): JsValue = JsObject(
      "type" -> JsString("MultiPoint"),
      "coordinates" ->
        JsArray(MultiPoint(obj).points.map { p => writeJtsPointCoords(p.jtsGeom) }.toVector)
    )
  }

  implicit object JtsMultiLineFormat extends RootJsonFormat[jts.MultiLineString] {
    override def read(json: JsValue): jts.MultiLineString =
      json.asJsObject.getFields("type", "coordinates") match {
        case Seq(JsString("MultiLineString"), linesArray: JsArray) =>
          factory.createMultiLineString(linesArray.elements.map(readJtsLineCoords).toArray)
        case Seq(JsString("Feature")) =>
          read(unwrapFeature(json))
        case _ => throw new DeserializationException("MultiLine geometry expected")
      }

    override def write(obj: jts.MultiLineString): JsValue = JsObject(
      "type" -> JsString("MultiLineString"),
      "coordinates" ->
        JsArray(MultiLine(obj).lines.map { l => writeJtsLineCoords(l.jtsGeom) }.toVector)
    )
  }

  implicit object JtsMultiPolygonFormat extends RootJsonFormat[jts.MultiPolygon] {
    override def read(json: JsValue): jts.MultiPolygon =
      json.asJsObject.getFields("type", "coordinates") match {
        case Seq(JsString("MultiPolygon"), polygons: JsArray) =>
          factory.createMultiPolygon(polygons.elements.map(readJtsPolygonCoords).toArray)
        case Seq(JsString("Feature")) =>
          read(unwrapFeature(json))
        case _ => throw new DeserializationException("MultiPolygon geometry expected")
      }

    override def write(obj: jts.MultiPolygon): JsValue =  JsObject(
      "type" -> JsString("MultiPolygon"),
      "coordinates" ->
        JsArray(MultiPolygon(obj).polygons.map { p => writeJtsPolygonCoords(p.jtsGeom) }.toVector)
    )
  }

  implicit object JtsGeometryCollectionFormat extends RootJsonFormat[jts.GeometryCollection] {
    def write(gc: jts.GeometryCollection) = JsObject(
      "type" -> JsString("GeometryCollection"),
      "geometries" -> {
        val geomsJsons =
          for(i <- 0 until gc.getNumGeometries) yield {
            JtsGeometryFormat.write(gc.getGeometryN(i))
          }

        JsArray(geomsJsons.toVector)
      }
    )

    def read(value: JsValue) = value.asJsObject.getFields("type", "geometries") match {
      case Seq(JsString("GeometryCollection"), JsArray(geomsJson)) =>
        factory.createGeometryCollection(geomsJson.map(g => JtsGeometryFormat.read(g)).toArray)
      case Seq(JsString("Feature")) =>
        read(unwrapFeature(value))
      case _ => throw new DeserializationException("GeometryCollection expected")
    }
  }

  implicit object JtsGeometryFormat extends RootJsonFormat[jts.Geometry] {
    def write(geom: jts.Geometry) = geom match {
      case geom: jts.Point => geom.toJson
      case geom: jts.LineString => geom.toJson
      case geom: jts.Polygon => geom.toJson
      case geom: jts.MultiPolygon => geom.toJson
      case geom: jts.MultiPoint => geom.toJson
      case geom: jts.MultiLineString => geom.toJson
      case geom: jts.GeometryCollection => geom.toJson
      case _ => throw new SerializationException("Unknown Geometry type ${geom.getClass.getName}: $geom")
    }

    def read(value: JsValue) = value.asJsObject.getFields("type") match {
      case Seq(JsString("Feature")) => read(unwrapFeature(value))
      case Seq(JsString("Point")) => value.convertTo[jts.Point]
      case Seq(JsString("LineString")) => value.convertTo[jts.LineString]
      case Seq(JsString("Polygon")) => value.convertTo[jts.Polygon]
      case Seq(JsString("MultiPolygon")) => value.convertTo[jts.MultiPolygon]
      case Seq(JsString("MultiPoint")) => value.convertTo[jts.MultiPoint]
      case Seq(JsString("MultiLineString")) => value.convertTo[jts.MultiLineString]
      case Seq(JsString("GeometryCollection")) => value.convertTo[jts.GeometryCollection]
      case Seq(JsString(t)) => throw new DeserializationException(s"Unknown Geometry type: $t")
    }
  }

  /** Unwrap feature geometry (ignoring its properties) */
  private def unwrapFeature(value: JsValue): JsValue = {
    value.asJsObject.getFields("type", "geometry") match {
      case Seq(JsString("Feature"), geom) =>  geom
      case _ => value
    }
  }
}

object JtsGeometryFormats extends JtsGeometryFormats
