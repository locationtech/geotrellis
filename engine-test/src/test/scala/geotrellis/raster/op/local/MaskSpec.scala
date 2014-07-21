/*
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
 */

package geotrellis.raster.op.local

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.feature._
import geotrellis.feature.json._
import geotrellis.testkit._

import org.scalatest._

import spray.json.DefaultJsonProtocol._

class MaskSpec extends FunSpec 
                  with Matchers 
                  with TestEngine 
                  with TileBuilders {
  describe("Mask") {
    it("should work with integers") {
            val rs1 = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      val rs2 = createRasterSource(
        Array( 0,0,0, 0,0,0, 0,0,0,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               0,0,0, 0,0,0, 0,0,0),
        3,2,3,2)

      val r1 = get(rs1)
      run(rs1.localMask(rs2, 2, NODATA)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row != 0 && row != 3)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (r1.get(col,row))
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }

  describe("mask method") {
    it("works over a tiled raster") {
      val geoJson = """
{"type":"Feature", "properties":{}, "geometry":{"type":"Polygon", "coordinates":[[[-8369090.2257790025, 4876802.227461054], [-8387435.112564891, 4875204.103461534], [-8388505.230960803, 4857043.545520123], [-8381778.772472589, 4846680.974002159], [-8365574.122478416, 4850266.796002782], [-8362516.641347379, 4876202.9007538855], [-8365956.307619781, 4884296.872970244], [-8369090.2257790025, 4876802.227461054]]]}, "crs":{"type":"name", "properties":{"name":"urn:ogc:def:crs:OGC:1.3:CRS84"}}}
"""
      val poly = geoJson.parseGeoJson[PolygonFeature[Unit]]

      val re = RasterSource("SBN_inc_percap_tiled").rasterExtent.get
      val rs = RasterSource("SBN_inc_percap_tiled")
      val raster = rs.get

      val masked = rs.mask(poly).get

      // rasterize.Rasterizer.foreachCellByFeature(g, re)(new rasterize.Callback {
      //   def apply(col: Int, row: Int) = {
      //     println(
      //   }
      //     result.setDouble(col, row, tile.getDouble(col, row))
      // })

      for(col <- 0 until raster.cols;
          row <- 0 until raster.rows) {
        val rvalue = raster.getDouble(col,row)
        val maskedValue = masked.getDouble(col,row)

        val (x,y) = re.gridToMap(col,row)

        if(Point(x,y).intersects(poly)) {
          if(isData(rvalue)) maskedValue should be (rvalue)
          else isNoData(maskedValue) should be (true)
        } else {
          withClue(s"Value at ($col, $row) is $maskedValue, should be NoData") {
            //isNoData(maskedValue) should be (true)
            if(!isNoData(maskedValue)) {
              println(s"BAD ${Point(x, y)} not in $poly")
            }
          }
        }
      }
    }
  }
}
