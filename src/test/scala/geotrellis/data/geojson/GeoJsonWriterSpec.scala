/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.data.geojson

import geotrellis._
import geotrellis.feature._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.vividsolutions.jts.{ geom => jts }

class GeoJsonWriterSpec extends FunSpec
                           with ShouldMatchers {
  def removews(s:String) = """[\s]+""".r.replaceAllIn(s, m => "")

  def c(x:Double,y:Double) = new jts.Coordinate(x,y)

  describe("GeoJsonWriter") {
    it("should write proper geojson for Point") {
      val gj =
        GeoJsonWriter.createString(
          Point(1.5,2.5,"thepoint")
        )

      removews(gj) should be {
        removews(
        """ 
        { 
           "type": "Feature",
           "geometry": {
              "type": "Point",
              "coordinates": [1.5,2.5] 
           },
           "properties": { "data": "thepoint" }
         }
        """
        )
      }
    }

    it("should write proper geojson for LineString") {
      val gj = 
        GeoJsonWriter.createString(
          LineString(Seq((100.0,0.0),(101.0,1.0)),"thelinestring")
        )

      removews(gj) should be {
        removews(
        """ 
        { 
           "type": "Feature",
           "geometry": {
              "type": "LineString",
              "coordinates": [ [100.0, 0.0], [101.0, 1.0] ] 
           },
           "properties": { "data": "thelinestring" }
         }
        """
        )
      }
    }

    it("should write proper geojson for Polygon") {
      val gj = 
        GeoJsonWriter.createString(
          Polygon(
            Array(c(100.0,0.0),c(101.0,0.0),c(101.0,1.0),c(100.0,1.0),c(100.0,0.0)),
            Array(
              Array(c(100.2,0.2),c(100.8,0.2),c(100.8,0.8),c(100.2,0.8),c(100.2,0.2))
            ),
            "thepolygon"
          )
        )

      removews(gj) should be {
        removews(
        """ 
        { "type": "Feature",
           "geometry": {
              "type": "Polygon",
              "coordinates": [
    [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
    [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]
    ]
           },
           "properties": { "data": "thepolygon" }
         }
        """
        )
      }
    }

    it("should write proper geojson for MultiPoint") {
      val gj = 
        GeoJsonWriter.createString(
          MultiPoint(Seq(Seq(100.0,0.0),Seq(101.0,1.0)),"themultipoint")
        )

      removews(gj) should be {
        removews(
        """ 
        { "type": "Feature",
           "geometry": {
              "type": "MultiPoint",
              "coordinates": [ [100.0, 0.0], [101.0, 1.0] ]
           },
           "properties": { "data": "themultipoint" }
         }
        """
        )
      }
    }

    it("should write proper geojson for MultiLineString") {
      val gj = 
        GeoJsonWriter.createString(
          MultiLineString(
            Seq(
              Seq(
                Seq(100.0,0.0),Seq(101.0,1.0)
              ),
              Seq(
                Seq(102.0,2.0),Seq(103.0,3.0)
              )
            ), "themultilinestring")
        )

      removews(gj) should be {
        removews(
        """ 
        { "type": "Feature",
           "geometry": {
              "type": "MultiLineString",
              "coordinates": [
                 [ [100.0, 0.0], [101.0, 1.0] ],
                 [ [102.0, 2.0], [103.0, 3.0] ]
               ]
           },
           "properties": { "data": "themultilinestring" }
         }
        """
        )
      }
    }

    it("should write proper geojson for MultiPolygon") {
      val gj = 
        GeoJsonWriter.createString(
          MultiPolygon(
            Seq(
              //First polygon
              Seq(
                //Shell
                Seq(
                  Seq(102.0,2.0),Seq(103.0,2.0),Seq(103.0,3.0),Seq(102.0,3.0),Seq(102.0,2.0)
                )
              ),
              //Second Polygon
              Seq(
                //Shell
                Seq(
                  Seq(100.0,0.0),Seq(101.0,0.0),Seq(101.0,1.0),Seq(100.0,1.0),Seq(100.0,0.0)
                ),
                //Hole
                Seq(
                  Seq(100.2,0.2),Seq(100.8,0.2),Seq(100.8,0.8),Seq(100.2,0.8),Seq(100.2,0.2)
                )
              )
            ), "themultipolygon")
        )

      removews(gj) should be {
        removews(
        """ 
        { "type": "Feature",
           "geometry": {
              "type": "MultiPolygon",
              "coordinates": [
                 [[[102.0, 2.0], [103.0, 2.0], [103.0, 3.0], [102.0, 3.0], [102.0, 2.0]]],
                 [[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]],
                 [[100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2]]]
               ]
           },
           "properties": { "data": "themultipolygon" }
         }
        """
        )
      }
    }

    it("should write proper geojson for geometry collection") {
      val gj = 
        GeoJsonWriter.createString(
          new GeometryCollection(
            new jts.GeometryCollection(
              Array(
                Feature.factory.createPoint(new jts.Coordinate(15.2,10.9)),
                Feature.factory.createLineString(
                  Array(
                    new jts.Coordinate(100.0,0.0),
                    new jts.Coordinate(101.0,1.0),
                    new jts.Coordinate(102.0,0.0)
                  )
                )
              ), 
              Feature.factory
            ),
            "thegeometrycollection"
          )
        )

      val expected = 
        """ 
        { "type": "Feature",
           "geometry": {
              "type": "GeometryCollection",
              "geometries": [
                { "type": "Point",
                  "coordinates": [15.2,10.9]
                },
                { "type": "LineString",
                  "coordinates": [ [100.0, 0.0], [101.0,1.0], [102.0,0.0] ]
                }
              ]
           },
           "properties": { "data": "thegeometrycollection" }
         }
        """

      withClue(s"Actual:\n$gj\n\nExpected:\n$expected") { 
        removews(gj) should be (removews(expected))
      }
    }

    it("should write proper geojson for feature collection") {
      val gj = 
        GeoJsonWriter.createFeatureCollectionString(
          List[Geometry[String]](
            Point(1.5,2.5,"thepoint"),
            Polygon(
              Array(
                c(100.0,0.0),c(101.0,0.0),c(101.0,1.0),c(100.0,1.0),c(100.0,0.0)
              ),
              Array(
                Array(
                  c(100.2,0.2),c(100.8,0.2),c(100.8,0.8),c(100.2,0.8),c(100.2,0.2)
                )
              ),
              "thepolygon"
            ),
            LineString(Seq((100.0,0.0),(101.0,1.0)),"thelinestring")
          ),
          false
        )

      removews(gj) should be {
        removews(
        """ 
          { "type": "FeatureCollection",
            "features": [
              { 
                 "type": "Feature",
                 "geometry": {
                    "type": "Point",
                    "coordinates": [1.5,2.5] 
                 }
               },
              { "type": "Feature",
                 "geometry": {
                    "type": "Polygon",
                    "coordinates": [
          [ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0] ],
          [ [100.2, 0.2], [100.8, 0.2], [100.8, 0.8], [100.2, 0.8], [100.2, 0.2] ]
          ]
                 }
               },        
              { 
           "type": "Feature",
                 "geometry": {
                    "type": "LineString",
                    "coordinates": [ [100.0, 0.0], [101.0, 1.0] ] 
                 }
               }
            ]
          }
          """
        )
      }
    }
  }
}
