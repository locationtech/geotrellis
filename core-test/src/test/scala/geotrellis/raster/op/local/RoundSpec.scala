/***
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
 ***/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class RoundSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Round") {    
    it("takes round of int tiled RasterSource") {
      val rs = createRasterSource(
        Array( NODATA,20,20, 20,20,20, 20,20,20,
               20,20,20, 20,20,20, 20,20,20,

               20,20,20, 20,20,20, 20,20,20,
               20,20,20, 20,20,20, 20,20,20),
        3,2,3,2)

      run(rs.localRound) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (20)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("takes round of Double tiled RasterSource") {
      val rs = createRasterSource(
        Array( Double.NaN,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,
               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,

               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2,
               34.2,34.2,34.2, 34.2,34.2,34.2, 34.2,34.2,34.2),
        3,2,3,2)

      run(rs.localRound) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                isNoData(result.getDouble(col,row)) should be (true)
              else
                result.getDouble(col,row) should be (34.0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Round on Raster") {
    it("correctly rounds a Int Raster") {
      val r = createRaster(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1), 9, 4)
      val result = get(r.localRound())
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if (row == 0 && col == 0) {
            result.get(col,row) should be (NODATA)
          }
          else {
            result.get(col,row) should be (math.round(r.get(col,row)).toInt)
          }
        }
      }
    }
    it("correctly rounds a Double Raster") {
      val r = createRaster(
        Array( Double.NaN,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               1.2,1.2,1.2, 1.2,1.2,1.2, 1.2,1.2,1.2,

               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.8,1.8,1.8, 1.8,1.8,1.8, 1.8,1.8,1.8), 9, 4)
      val result = get(r.localRound())
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if (row == 0 && col == 0) {
            result.getDouble(col,row).isNaN should be (true)
          }
          else {
            result.getDouble(col,row) should be (math.round(r.getDouble(col,row)))
          }
        }
      }
    }
  }
}
