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

import org.scalatest._

import geotrellis.testkit._

class DefinedSpec extends FunSpec 
                     with Matchers 
                     with TestServer 
                     with RasterBuilders {
  describe("Defined") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = get(Defined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = get(Defined(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }

    it("returns correct result for a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(rs.localDefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rasterExtent.rows / 3) {
            for(col <- 0 until r.rasterExtent.cols / 3) {
              val z = r.getDouble(col,row)
              val rz = result.get(col,row)
              if(isNoData(z))
                rz should be (0)
              else 
                rz should be (1)
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

      run(rs.localDefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (0)
              else
                result.get(col,row) should be (1)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Defined raster method") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.localDefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = r.localDefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (0)
          else result.get(col,row) should be (1)
        }
      }
    }
  }
}
