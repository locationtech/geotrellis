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

class DivideSpec extends FunSpec 
                      with ShouldMatchers 
                      with TestServer 
                      with RasterBuilders {
  describe("Divide") {
    it("divides a constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = get(Divide(r,5))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) / 5)
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = get(Divide(r,3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 3)
        }
      }
    }

    it("divides a constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = get(Divide(-10,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-10 / r.get(col,row))
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = get(Divide(-3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.0 / r.getDouble(col,row))
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = get(Divide(r,5.1))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) / 5.1).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = get(Divide(r,.3))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 0.3)
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = get(Divide(-10.7,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (-10.7 / r.get(col,row)).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = get(Divide(-3.3,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.3 / r.getDouble(col,row))
        }
      }
    }

    it("divides an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = get(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (1)
        }
      }
    }
    
    it("divides a double raster to itself") {
      val r = probabilityRaster
      val result = get(Divide(r,r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (1.0)
        }
      }
    }

    it("divides two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2")

      val r1 = get(rs1)
      val r2 = get(rs2)
      run(rs1 / rs2) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until r1.rasterExtent.rows) {
            for(col <- 0 until r1.rasterExtent.cols) {
              if(isNoData(result.get(col,row))) {
                (isNoData(r1.get(col,row)) ||
                 isNoData(r2.get(col,row)) ||
                 r2.get(col,row) == 0) should be (true)
              } else {
                result.get(col,row) should be (r1.get(col,row) / r2.get(col,row))
              }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("divides three tiled RasterSources correctly") {
      val rs1 = createRasterSource(
        Array( 1000,1000,1000, 1000,1000,1000, 1000,1000,1000,
               1000,1000,1000, 1000,1000,1000, 1000,1000,1000,

               1000,1000,1000, 1000,1000,1000, 1000,1000,1000,
               1000,1000,1000, 1000,1000,1000, 1000,1000,1000),
        3,2,3,2)

      val rs2 = createRasterSource(
        Array( 200,200,200, 200,200,200, 200,200,200,
               200,200,200, 200,200,200, 200,200,200,

               200,200,200, 200,200,200, 200,200,200,
               200,200,200, 200,200,200, 200,200,200),
        3,2,3,2)

      val rs3 = createRasterSource(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        3,2,3,2)

      run(rs1 / rs2 / rs3) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              result.get(col,row) should be ((1000/200)/2)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
