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

package geotrellis.raster

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class RasterDataSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("convert") {
    it("should convert a byte raster to an int raster") { 
      val r = byteRaster
      var result = r.convert(TypeShort)
                    .map { z => z + 100 }
      result.rasterType should be (TypeShort)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 100)
        }
      }
    }
  }

  describe("mapIfSet") {
    def check(r:Raster) = {
      val r2 = r.mapIfSet(z => 1)
      val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val v = r.get(col,row)
          val v2 = r2.get(col,row)
          if(isNoData(v)) {
            v2 should be (NODATA)
          }
        }
      }

      val r3 = r.mapIfSetDouble(z => 1.0)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val v = r.getDouble(col,row)
          val v3 = r3.getDouble(col,row)
          if(isNoData(v)) {
            isNoData(v3) should be (true)
          }
        }
      }
    }

    it("should respect NoData values") {
      withClue("ByteArrayRasterData") { check(byteNoDataRaster) }
      withClue("ShortArrayRasterData") { 
        val n = shortNODATA
        check(createRaster(Array[Short](1,2,3,n,n,n,3,4,5)))
      }
      withClue("IntArrayRasterData") { check(positiveIntegerNoDataRaster) }
      withClue("FloatArrayRasterData") { 
        val n = Float.NaN
        check(createRaster(Array[Float](1.0f,2.0f,3.0f,n,n,n,3.0f,4.0f,5.0f)))
      }
    }
  }

  describe("warp") {
    it("should warp with crop only") {
      val rd = RasterData(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4)
      val re = RasterExtent(Extent(0.0,0.0,9.0,4.0),9,4)
      val nre = RasterExtent(Extent(0.0,1.0,4.0,4.0),4,3)
      rd.warp(re,nre).toArray should be (Array(1,10,100,1000,
                                               2,20,200,2000,
                                               3,30,300,3000))
    }

    it("should give NODATA for warp with crop outside of bounds") {
      val rd = RasterData(
        Array( 1,10,100,1000,2,2,2,2,2,
               2,20,200,2000,2,2,2,2,2,
               3,30,300,3000,2,2,2,2,2,
               4,40,400,4000,2,2,2,2,2),
        9,4)
      val re = RasterExtent(Extent(0.0,0.0,9.0,4.0),9,4)
      val nre = RasterExtent(Extent(-1.0,2.0,3.0,5.0),1.0,1.0,4,3)
      println(nre)
      printR(Raster(rd.warp(re,nre),nre))
      rd.warp(re,nre).toArray should be (Array(nd,nd,nd, nd,
                                               nd, 1,10,100,
                                               nd, 2,20,200))
    }

    it("should warp with resolution decrease in X and crop in Y") {
      val rd = RasterData(
        Array( 1,10,100,1000,-2,2,2,2,2,
               2,20,200,2000,-2,2,2,2,2,
               3,30,300,3000,-2,2,2,2,2,
               4,40,400,4000,-2,2,2,2,2),
        9,4)
      val re = RasterExtent(Extent(0.0,0.0,9.0,4.0),9,4)
      val nre = RasterExtent(Extent(0.0,1.0,9.0,4.0),3,3)
      rd.warp(re,nre).toArray should be (Array(10,-2,2,
                                               20,-2,2,
                                               30,-2,2))
    }
  }
}
