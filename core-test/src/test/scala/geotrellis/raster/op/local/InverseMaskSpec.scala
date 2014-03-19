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

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import scala.math.min

import geotrellis.testkit._

class InverseMaskSpec extends FunSpec 
                  with ShouldMatchers 
                  with TestServer 
                  with RasterBuilders {
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
      run(rs1.localInverseMask(rs2, 2, NODATA)) match {
        case Complete(result,success) =>
          printR(result)
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 || row == 3)
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
  describe("Inverse Mask on rasters") {
    it("should work with integers") {
      val r1 = createRaster(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1), 9, 4)

      val r2 = createRaster(
        Array( 0,0,0, 0,0,0, 0,0,0,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               0,0,0, 0,0,0, 0,0,0), 9, 4)

        val result = get(r1.localInverseMask(r2, 2, NODATA))
        for(row <- 0 until 4) {
          for(col <- 0 until 9) {
            if(row == 0 || row == 3)
              result.get(col,row) should be (NODATA)
            else
              result.get(col,row) should be (r1.get(col,row))
          }
        }
    }
    it("should work with doubles") {
      val r1 = createRaster(
        Array( Double.NaN,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               2.0,3.0,4.0, 5.0,6.0,7.0, 8.0,9.0,0.0,
               1.0,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               1.0,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0), 9, 4)

      val r2 = createRaster(
        Array( 0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0,
               2.0,2.0,2.0, 2.0,2.0,2.0, 2.0,2.0,2.0,
               2.0,2.0,2.0, 2.0,2.0,2.0, 2.0,2.0,2.0,
               0.0,0.0,0.0, 0.0,0.0,0.0, 0.0,0.0,0.0), 9, 4)

        val result = get(r1.localInverseMask(r2, 2, NODATA))
        for(row <- 0 until 4) {
          for(col <- 0 until 9) {
            if (row == 0 && col == 0)
              result.getDouble(col, row).isNaN should be (true)
            else if(row == 0 || row == 3)
              result.getDouble(col,row).isNaN should be (true)
            else
              result.getDouble(col,row) should be (r1.get(col,row))
          }
        }
    }
  }
}
