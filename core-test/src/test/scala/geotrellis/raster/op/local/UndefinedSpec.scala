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

class UndefinedSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Undefined") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = get(Undefined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = get(Undefined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }

    it("returns correct result for a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(rs.localUndefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.getDouble(col,row)
              val rz = result.get(col,row)
              if(isNoData(z))
                rz should be (1)
              else 
                rz should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("returns correct result for a int raster source") {
      val rs = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(rs.localUndefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (1)
              else
                result.get(col,row) should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Undefined raster method") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.localUndefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = r.localUndefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }
  }
}
