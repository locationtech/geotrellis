package geotrellis.data.geojson

import org.codehaus.jackson._
import org.codehaus.jackson.JsonToken._
import org.codehaus.jackson.map._

import geotrellis._
import geotrellis.feature._

import scala.collection.mutable.ListBuffer

object GeoJsonReader {
  val debug = true 
  val parserFactory = new MappingJsonFactory()

  /**
   * Parse a GeoJson string and return feature objects.
   *
   * If a feature GeoJson is provided, the JsonNode representation of 
   * the feature object in the GeoJson will be the data of the feature. 
   *
   * If the the parsing fails, None will be returned.
   */
  def parse(geojson:String):Option[Array[Geometry[Option[JsonNode]]]] = { 
    val parser = parserFactory.createJsonParser(geojson)

    // parse state vars
    var geometryType = ""
    var readCoordinates = false
    var coordinateArray:Option[List[Any]] = None
    var properties:Option[JsonNode] = None

    // result features
    var featureArray = ListBuffer[Geometry[Option[JsonNode]]]() 

    var geometryArray = ListBuffer[Geometry[Option[JsonNode]]]() 
    // is this a feature collection?
    var featureCollection = false

    // is this a geometry collection?
    var geometryCollection = false
  
    // track of how many objects deep we've descended
    var objectCounter = 0

    var collectionLevel = 0
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
                if (typeText != "feature" && typeText != "geometrycollection" && typeText != "featurecollection" && typeText != "features") {
                  geometryType = typeText
                } else {
                  if (typeText == "featurecollection") {
                    featureCollection = true
                    collectionLevel = objectCounter
                  }
                  if (typeText == "geometrycollection") {
                    geometryCollection = true
                    collectionLevel = objectCounter
                  }
                  if (typeText == "features") {
                    parser.nextToken()
                    //features = parser.readValueAsTree()
                  }
                  if (typeText == "geometries") {
                    parser.nextToken()
                  }
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
                if (featureCollection || geometryCollection) {
                  objectCounter += 1
                }
                parser.nextToken()
              }
              case "bbox" => {
                // we don't do anything with bbox as it doesn't
                // change the definition of the features
              }
              case "features" => {
                parser.nextToken()
                parser.nextToken()
                objectCounter += 1
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
            if (featureCollection || geometryCollection) {
              objectCounter += 1
              token = parser.nextToken()
            } else {
              val o = parser.readValueAsTree(); 
              token = parser.getCurrentToken() 
            }
          }
          case END_OBJECT => {
            objectCounter -= 1
            if (featureCollection || geometryCollection) {
              if (collectionLevel == objectCounter) {
                val featureZ = makeFeature(geometryType, coordinateArray, properties)
                val feature = featureZ(0)
                if (geometryCollection) {
                  geometryArray += feature
                } else {
                  featureArray += feature
                }
               }
            }
            if (geometryCollection && featureCollection && objectCounter == -1) {
              val geoms = geometryArray.map( _.geom )
              val f = GeometryCollection(geoms, properties)
              featureArray += f
              geometryCollection = false 
            }
            token = parser.nextToken() 
          }
          case _ => { token = parser.nextToken() }
        }
      }
      if (featureCollection) {
          Some(featureArray.toArray)
      } else if (geometryCollection) {
          val geoms = geometryArray.map( _.geom )

          val f = GeometryCollection(geoms, properties)
          Some(Array(f))
      } else {
        Some(makeFeature(geometryType, coordinateArray, properties))
      }
    } else {
      None
    } 
  }

    def makeFeature (geometryType:String, coordinateArray:Option[List[Any]], properties:Option[JsonNode]) = { 
      val feature:Array[Geometry[Option[JsonNode]]] = geometryType match {
        case "polygon" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[Double]]]] 
          // Complete the LineStrings by including the first element as the
          // last (not required by geojson)

          val polyCoords = coords.map ( closeLineString(_) )
          Array(Polygon(polyCoords, properties))
        }
        case "multipolygon" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[List[Double]]]]] 
          val multipolyCoords = coords.map(_.map(closeLineString(_)))
          Array(MultiPolygon(multipolyCoords, properties))
        }
        case "point" => {
          val coords = coordinateArray.get.asInstanceOf[List[Double]]
          Array(Point(coords(0), coords(1),properties))
        }
        case "multipoint" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[Double]]]
          Array(MultiPoint(coords, properties))
        }
        case "linestring" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[Double]]]
          if (coords.length % 2 != 0) {
            throw new Exception("couldn't parse linestring: coordinate length not divisible by 2")
          } else {
            val pts = for(p <- coords) yield { (p(0),p(1)) }
            Array(LineString(pts, properties))
          }
        }
        case "multilinestring" => {
          val coords = coordinateArray.get.asInstanceOf[List[List[List[Double]]]]
          Array(MultiLineString(coords, properties))
        }
      }
      feature
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

