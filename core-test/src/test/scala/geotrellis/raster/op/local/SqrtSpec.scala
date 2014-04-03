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
import geotrellis.process._
import geotrellis.source._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class SqrtSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Sqrt") {
    it("takes the square root of an integer raster") {
      val r = positiveIntegerRaster
      val result = get(Sqrt(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.sqrt(r.get(col,row)).toInt)
        }
      }
    }

    it("takes the square root of a double raster") {
      val r = probabilityRaster
      val result = get(Sqrt(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.sqrt(r.getDouble(col,row)))
        }
      }
    }

    it("takes the square root of a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(rs.localSqrt) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.get(col,row)
              val rz = result.getDouble(col,row) 
              if(isNoData(z) || z < 0.0)
                isNoData(rz) should be (true)
              else 
                rz should be (math.sqrt(z) plusOrMinus 1e-5)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("takes the square root of a int raster source") {
      val rs = createRasterSource(
        Array( NODATA,9,9, 9,9,9, 9,9,9,
               9,9,9, 9,9,9, 9,9,9,

               9,9,9, 9,9,9, 9,9,9,
               9,9,9, 9,9,9, 9,9,9),
        3,2,3,2)

      run(rs.localSqrt) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (3)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Sqrt raster method") {
    it("takes the square root of an integer raster") {
      val r = positiveIntegerRaster
      val result = r.localSqrt()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.sqrt(r.get(col,row)).toInt)
        }
      }
    }

    it("takes the square root of a double raster") {
      val r = probabilityRaster
      val result = r.localSqrt()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.sqrt(r.getDouble(col,row)))
        }
      }
    }
  }
}
