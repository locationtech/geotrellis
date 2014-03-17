/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package geotrellis.data.geojson

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

class GeoJsonReaderSpec extends FunSpec with MustMatchers with ShouldMatchers {
  describe("GeoJsonReader") {
  
    // Polygon feature geojson example
    val geojsonPolygonFeature = """
{"type":"Feature", "properties":{}, "geometry":{"type":"Polygon", "coordinates":[[[-102.89062544703, 42.447921037674], [-103.59375044703, 36.822921037674], [-94.453125447035, 35.416671037674], [-90.937500447035, 40.338546037674], [-96.562500447035, 44.557296037674], [-102.89062544703, 42.447921037674]]]}, "crs":{"type":"name", "properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}}}
    """

    // Polygon geometry geojson example
    val geojsonPolygonGeometry = """
{"type":"Polygon", "coordinates":[[[-102.89062544703, 42.447921037674], [-103.59375044703, 36.822921037674], [-94.453125447035, 35.416671037674], [-90.937500447035, 40.338546037674], [-96.562500447035, 44.557296037674], [-102.89062544703, 42.447921037674]]]}
 """

    // Some examples below from http://www.geojson.org/geojson-spec.html

    // MultiPolygon geometry geojson example
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
  val geojsonPointGeometry = """
{ "type": "Point", "coordinates": [100.0, 0.0] }
"""

  // MultiPoint geometry geojson example
  val geojsonMultiPointGeometry = """
{ "type": "MultiPoint",
  "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
 }
"""
  
  // LineString geometry geojson example
  val geojsonLineStringGeometry = """
{ "type": "LineString",
  "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
  }
"""

  // LineString geometry with multiple points
  val geojsonLineStringGeometry2 = """
{ "type": "LineString",
  "coordinates": [ [100.0, 0.0], [101.0, 1.0], [101.0, 7.0], [150.0, 10.0]]
  }
"""
  // MultiLine string example
  val geojsonMultiLineStringGeometry = """
{ "type": "MultiLineString",
  "coordinates": [
      [ [100.0, 0.0], [101.0, 1.0] ],
      [ [102.0, 2.0], [103.0, 3.0] ]
    ]
}
"""

  // geometry collection
  val geojsonGeometryCollection = """
{ "type": "GeometryCollection",
  "geometries": [
    { "type": "Point",
      "coordinates": [100.0, 0.0]
      },
    { "type": "LineString",
      "coordinates": [ [101.0, 0.0], [102.0, 1.0] ]
      }
  ]
}
  """

  val geojsonBothCollections = """
  { "type": "FeatureCollection",
    "features": [
      { "type": "GeometryCollection",
        "geometries": [
          { "type": "Point",
            "coordinates": [100.0, 0.0]
          },
          { "type": "LineString",
            "coordinates": [ [101.0, 0.0], [102.0, 1.0] ]
          }
        ],
        "properties": {"prop0": "value0"}
      }
    ]
  }
  """

  // FeatureCollection geometry example
  val geojsonFeatureCollection = """
{ "type": "FeatureCollection",
  "features": [
    { "type": "Feature",
      "geometry": {"type": "Point", "coordinates": [102.0, 0.5]},
      "properties": {"prop0": "value0"}
      },
    { "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [102.0, 0.0], [103.0, 1.0], [104.0, 0.0], [105.0, 1.0]
          ]
        },
      "properties": {
        "prop0": "value0",
        "prop1": 0.0
        }
      },
    { "type": "Feature",
       "geometry": {
         "type": "Polygon",
         "coordinates": [
           [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],
             [100.0, 1.0], [100.0, 0.0] ]
           ]
       },
       "properties": {
         "prop0": "value0",
         "prop1": {"this": "that"}
         }
       }
     ]
   }
  """


    it("should parse a Polygon feature") {
      val result = GeoJsonReader.parse(geojsonPolygonFeature)
      val polygonArray = result.get 
      println(polygonArray)
      val polygon = polygonArray(0)
      polygon.toString must be === "JtsPolygon(POLYGON ((-102.890625 42.44792175292969, -103.59375 36.82292175292969, -94.453125 35.41667175292969, -90.9375 40.33854675292969, -96.5625 44.55729675292969, -102.890625 42.44792175292969)),Some({}))"
    }

    it("should parse a Polygon geometry") {
      val result = GeoJsonReader.parse(geojsonPolygonGeometry)
      val polygonArray = result.get
      val polygon = polygonArray(0)
      polygon.toString must be === "JtsPolygon(POLYGON ((-102.890625 42.44792175292969, -103.59375 36.82292175292969, -94.453125 35.41667175292969, -90.9375 40.33854675292969, -96.5625 44.55729675292969, -102.890625 42.44792175292969)),None)"
    } 

    it("should parse a MultiPolygon geometry") {
      val result = GeoJsonReader.parse(geojsonMultiPolygonGeometry)
      result.get.apply(0).toString must be === "JtsMultiPolygon(MULTIPOLYGON (((102 2, 103 2, 103 3, 102 3, 102 2)), ((100 0, 101 0, 101 1, 100 1, 100 0), (100.19999694824219 0.2000000029802322, 100.80000305175781 0.2000000029802322, 100.80000305175781 0.800000011920929, 100.19999694824219 0.800000011920929, 100.19999694824219 0.2000000029802322))),None)"
    }

    it("should parse a Point geometry") {
      val result = GeoJsonReader.parse(geojsonPointGeometry) 
      result.get.apply(0).toString must be === "JtsPoint(POINT (100 0),None)"
    }

    it("should parse a MultiPoint geometry") {
      val result = GeoJsonReader.parse(geojsonMultiPointGeometry) 
      result.get.apply(0).toString must be === "JtsMultiPoint(MULTIPOINT ((100 0), (101 1)),None)"
    }

    it ("should parse a LineString geometry") {
      val result = GeoJsonReader.parse(geojsonLineStringGeometry)
      result.get.apply(0).toString must be === "JtsLineString(LINESTRING (100 0, 101 1),None)"
    }

    it ("should parse a LineString geometry with multiple points") {
      val result = GeoJsonReader.parse(geojsonLineStringGeometry2)
      result.get.apply(0).toString must be === "JtsLineString(LINESTRING (100 0, 101 1, 101 7, 150 10),None)"
    }

    it ("should parse a MultiLineString geometry") {
      val result = GeoJsonReader.parse(geojsonMultiLineStringGeometry)
      result.get.apply(0).toString must be === "JtsMultiLineString(MULTILINESTRING ((100 0, 101 1), (102 2, 103 3)),None)"
    }

    it("should parse a feature collection") {
      val result = GeoJsonReader.parse(geojsonFeatureCollection).get
      result.length should be === 3
      result.apply(0).toString must be === """JtsPoint(POINT (102 0.5),Some({"prop0":"value0"}))"""
      result.apply(1).toString must be === """JtsLineString(LINESTRING (102 0, 103 1, 104 0, 105 1),Some({"prop0":"value0","prop1":0.0}))"""
      result.apply(2).toString must be === """JtsPolygon(POLYGON ((100 0, 101 0, 101 1, 100 1, 100 0)),Some({"prop0":"value0","prop1":{"this":"that"}}))"""
    }

    it("should parse a GeometryCollection") {
      val result = GeoJsonReader.parse(geojsonGeometryCollection).get
      result.length should be === 1
      result.apply(0).toString must be === """JtsGeometryCollection(GEOMETRYCOLLECTION (POINT (100 0), LINESTRING (101 0, 102 1)),None)"""
    }

    it("should parse a feature collection w/ a geometry collection") {
      val result = GeoJsonReader.parse(geojsonBothCollections).get
      result.length should be === 1
      result.apply(0).toString must be === """JtsGeometryCollection(GEOMETRYCOLLECTION (POINT (100 0), LINESTRING (101 0, 102 1)),Some({"prop0":"value0"}))"""
    }
  }
}
