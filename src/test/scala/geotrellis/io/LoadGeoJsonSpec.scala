package geotrellis.io


import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers
//import org.junit.runner.RunWith

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class LoadGeoJsonSpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("io.LoadGeoJson.parse") {

    // Polygon feature geojson example
    val geojsonPolygonFeature = """
{"type":"Feature", "properties":{}, "geometry":{"type":"Polygon", "coordinates":[[[-102.89062544703, 42.447921037674], [-103.59375044703, 36.822921037674], [-94.453125447035, 35.416671037674], [-90.937500447035, 40.338546037674], [-96.562500447035, 44.557296037674], [-102.89062544703, 42.447921037674]]]}, "crs":{"type":"name", "properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}}}
    """

    // Polygon geometry geojson example
    val geojsonPolygonGeometry = """
{"type":"Polygon", "coordinates":[[[-102.89062544703, 42.447921037674], [-103.59375044703, 36.822921037674], [-94.453125447035, 35.416671037674], [-90.937500447035, 40.338546037674], [-96.562500447035, 44.557296037674], [-102.89062544703, 42.447921037674]]]}
 """

    // MultiPolygon geometry geojson example
    // from http://www.geojson.org/geojson-spec.html
    val geojsonMultiPolygonGeometry = """
{ "type": "MultiPolygon",
  "coordinates": [
    [[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]],
    [[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
     [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]
    ]
  }
"""

  // Point geometry geojson example
  // from http://www.geojson.org/geojson-spec.html
  val geojsonPointGeometry = """
{ "type": "Point", "coordinates": [100.0, 0.0] }
"""

  // MultiPoint geometry geojson example
  // from http://www.geojson.org/geojson-spec.html
  val geojsonMultiPointGeometry = """
{ "type": "MultiPoint",
  "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
 }
"""
  
  // LineString geometry geojson example
  // from http://www.geojson.org/geojson-spec.html
  val geojsonLineStringGeometry = """
{ "type": "LineString",
  "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
  }
"""

  // MultiLineString geometry geojson example
  // from http://www.geojson.org/geojson-spec.html 
  val geojsonMultiLineStringGeometry = """
{ "type": "MultiLineString",
  "coordinates": [
      [ [100.0, 0.0], [101.0, 1.0] ],
      [ [102.0, 2.0], [103.0, 3.0] ]
    ]
  }"""


    it("should parse a Polygon feature") {
      val result = LoadGeoJson.parse(geojsonPolygonFeature)
      println("result is: " + result)
      val polygonArray = result.get 
      println(polygonArray)
      val polygon = polygonArray(0)
      polygon.toString must be === "JtsPolygon(POLYGON ((-102.890625 42.44792175292969, -103.59375 36.82292175292969, -94.453125 35.41667175292969, -90.9375 40.33854675292969, -96.5625 44.55729675292969, -102.890625 42.44792175292969)),Some({}))"
    }

    it("should parse a Polygon geometry") {
      val result = LoadGeoJson.parse(geojsonPolygonGeometry)
      val polygonArray = result.get
      val polygon = polygonArray(0)
      polygon.toString must be === "JtsPolygon(POLYGON ((-102.890625 42.44792175292969, -103.59375 36.82292175292969, -94.453125 35.41667175292969, -90.9375 40.33854675292969, -96.5625 44.55729675292969, -102.890625 42.44792175292969)),None)"
    } 

    it("should parse a MultiPolygon geometry") {
      val result = LoadGeoJson.parse(geojsonMultiPolygonGeometry)
      result.get.apply(0).toString must be === "JtsMultiPolygon(MULTIPOLYGON (((102 2, 103 2, 103 3, 102 3, 102 2)), ((100 0, 101 0, 101 1, 100 1, 100 0), (100.19999694824219 0.2000000029802322, 100.80000305175781 0.2000000029802322, 100.80000305175781 0.800000011920929, 100.19999694824219 0.800000011920929, 100.19999694824219 0.2000000029802322))),None)"
    }

    it("should parse a Point geometry") {
      val result = LoadGeoJson.parse(geojsonPointGeometry) 
      result.get.apply(0).toString must be === "JtsPoint(POINT (100 0),None)"
    }

    it("should parse a MultiPoint geometry") {
      val result = LoadGeoJson.parse(geojsonMultiPointGeometry) 
      result.get.apply(0).toString must be === "JtsMultiPoint(MULTIPOINT ((100 0), (101 1)),None)"
    }

    it ("should parse a LineString geometry") {
      val result = LoadGeoJson.parse(geojsonLineStringGeometry)
      result.get.apply(0).toString must be === "JtsLineString(LINESTRING (100 0, 101 1),None)"
    }

    it ("should parse a MultiLineString geometry") {
      val result = LoadGeoJson.parse(geojsonMultiLineStringGeometry)
      result.get.apply(0).toString must be === "JtsMultiLineString(MULTILINESTRING ((100 0, 101 1), (102 2, 103 3)),None)"
    }
  }
}
