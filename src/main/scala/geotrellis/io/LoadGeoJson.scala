package geotrellis.io

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import geotrellis._
import geotrellis.feature._

import scala.collection.mutable.ListBuffer

object LoadGeoJson {
  val debug = true 
  val parserFactory = new MappingJsonFactory()

  /**
   * Parse a GeoJson string and return feature objects.
   *
   * If a feature GeoJson is provided, the JsonNode representation of 
   * the feature object in the GeoJson will be the data of the feature. 
   *
   * If the the parsing fails, None will be returned.
   *
   * Feature and GeometryCollections are not yet supported by this method.
   */
  def parse(geojson:String):Option[Array[Geometry[Option[JsonNode]]]] = { 
    val parser = parserFactory.createJsonParser(geojson)
    var geometryType = ""
    var readCoordinates = false
    var coordinateArray:Option[List[Any]] = None
    var properties:Option[JsonNode] = None
    
    if (parser.nextToken() == START_OBJECT) {
      var token = parser.nextToken()
      // Main parsing loop
 
      // Because we accept both just the geometry JSON or an enclosing
      // feature JSON, we do not distinguish between the two in this loop. 
      while (token != null) {
        token match {
          case FIELD_NAME => {
            parser.getCurrentName() match {
              case "type" => {
                parser.nextToken()
                val typeText = parser.getText().toLowerCase
                if (typeText != "feature") {
                  geometryType = typeText
                }
              }
              case "properties" => {
                parser.nextToken()
                properties = Some(parser.readValueAsTree())
                parser.getCurrentToken()
              }
              case "coordinates" => {
                readCoordinates = true
              }
              case "geometry" => { 
                // We conflate the geometry object and the feature object 
                // so that we can handle either -- so we skip the  
                // start object token to follow.
                parser.nextToken()
              }
              case "bbox" => {
                // we don't do anything with bbox as it doesn't
                // change the definition of the features
              }
              case _ => {}
            }
            token = parser.nextToken
          }
          case START_ARRAY => { 
            token = parser.nextToken()
            if (readCoordinates) {
              val coords = parseArrays(parser)
              coordinateArray = Some(coords)
              readCoordinates = false
              token = parser.nextToken()
            } 
          }
          case START_OBJECT => { 
            // If we come upon an object that has not been
            // handled somewhere else, we call readValueAsTree() to
            // move beyond this object.
            val o = parser.readValueAsTree(); 
            token = parser.getCurrentToken() 
          }
          case END_OBJECT => token = parser.nextToken() 
          case _ => { token = parser.nextToken() }
        }
      }
      
      val feature:Option[Array[Geometry[Option[JsonNode]]]] = geometryType match {
        case "polygon" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[Double]]]] 
          // Complete the LineStrings by including the first element as the
          // last (not required by geojson)

          val polyCoords = coords.map ( closeLineString(_) )
          Some(Array(Polygon(polyCoords, properties)))
        }
        case "multipolygon" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[List[Double]]]]] 
          val multipolyCoords = coords.map(_.map(closeLineString(_)))
          Some(Array(MultiPolygon(multipolyCoords, properties)))
         //val coords = coordinateArray.get.asInstanceOf[List[List[List[
        }
        case "point" => {
          val coords = coordinateArray.get.asInstanceOf[List[Double]]
          Some(Array(Point(coords(0), coords(1),properties)))
        }
        case "multipoint" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[Double]]]
          Some(Array(MultiPoint(coords, properties)))
        }
        case "linestring" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[Double]]]
          if (coords.length != 2) {
            None
          } else {
            val x0 = coords(0)(0)
            val y0 = coords(0)(1)
            val x1 = coords(1)(0)
            val y1 = coords(1)(1)
            Some(Array(LineString(x0, y0, x1, y1, properties)))
          }
        }
        case "multilinestring" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[Double]]]]
          Some(Array(MultiLineString(coords, properties))) 
        }
      }
      feature
    } else {
      None
    }
  }

  def parseArrays(parser:org.codehaus.jackson.JsonParser):List[Any] = {
    var result:List[Any] = List[Any]()
    var floatArray:Boolean = false

    var token = parser.getCurrentToken()

    // Parse until end of the array
    while ( token != END_ARRAY && token != null) {
      token match {
        case START_ARRAY => {
          // We've come upon an inner array; recursively invoke parseArrays
          parser.nextToken()
          result = result :+ parseArrays(parser)
        }
        case VALUE_NUMBER_FLOAT => {
          // We've come upon an inner (double,double) coordinate array.
          val c0 = parser.getFloatValue()
          parser.nextToken()
          val c1 = parser.getFloatValue()
          result = List[Double](c0, c1) 
        }
        
        case _ => {
          throw new Exception("Found unexpected token in GeoJson Array " + parser.getCurrentToken()) 
        }
      } 
      token = parser.nextToken()
    }
    result
  }

  // JTS Polygons expect polygon ring linestrings to be closed, in the sense
  // that the first and last coordinates are the same.  GeoJSON does not 
  // specify this; so we have to manually close the linestrings.
  private def closeLineString(lineString: List[List[Double]]) = {
    if (lineString.head != lineString.last) {
      lineString :+ lineString.head
    } else {
      lineString
    }
  }
}

/**
 * Load a feature from GeoJson.
 *
 * This operation loads a feature from GeoJson.  It accepts both simple
 * geometry definitions and feature definitions.  If there is a property
 * JSON clause, the feature data will be Some(JsonNode).
 */
case class LoadGeoJsonFeature(geojson:Op[String]) extends Op1(geojson)({
  (geojson) => {
    val resultOpt = LoadGeoJson.parse(geojson)
    resultOpt match {
      case None => StepError("Could not parse GeoJSON", "")
      case Some(geometryArray) => {
        val geometryCount = geometryArray.length
        if (geometryCount != 1) {
          StepError("Expected a single feature; found %d features".format(geometryCount), "")
        } else {
          Result( geometryArray(0) )
        }
      }
    } 
  }
})
