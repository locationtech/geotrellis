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

class AndSpec extends FunSpec 
                 with Matchers 
                 with TestEngine 
                 with TileBuilders {
  describe("And") {
    it("ands an Int raster and a constant") {
      assertEqual(And(createValueRaster(10,9),3), createValueRaster(10,1))
    }

    it("ands two Int rasters") {
      val op = And(createValueRaster(10,9),createValueRaster(10,3))
      assertEqual(op, 
                  createValueRaster(10,1))
    }

    it("ands a Double raster and a constant") {
      assertEqual(And(createValueRaster(10,9.4),3), createValueRaster(10,1.0))
    }

    it("ands two Double rasters") {
      assertEqual(And(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,1.0))
    }

    it("ands three tiled RasterSources correctly") {
      val rs1 = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      val rs2 = createRasterSource(
        Array( 2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2,

               2,2,2, 2,2,2, 2,2,2,
               2,2,2, 2,2,2, 2,2,2),
        3,2,3,2)

      val rs3 = createRasterSource(
        Array( 3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3,

               3,3,3, 3,3,3, 3,3,3,
               3,3,3, 3,3,3, 3,3,3),
        3,2,3,2)

      run(rs1 & rs2 & rs3) match {
        case Complete(result,success) =>
          //println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (NODATA)
              else
                result.get(col,row) should be (1^2^3)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("And on Raster") {
    it("ands an Int raster and a constant") {
      assertEqual(createValueRaster(10,9) & 3, createValueRaster(10,1))
      assertEqual(createValueRaster(10,9) & 0, createValueRaster(10,0))
    }

    it("ands two Int rasters") {
      val result = createValueRaster(10,9) & (createValueRaster(10,3))
      assertEqual(result,
                  createValueRaster(10,1))
    }

    it("ands a Double raster and a constant") {
      assertEqual(createValueRaster(10,9.4) & (3), createValueRaster(10,1.0))
    }

    it("ands two Double rasters") {
      assertEqual(createValueRaster(10,9.9) & (createValueRaster(10,3.2)),
                  createValueRaster(10,1.0))
    }
    it("ands a Seq of rasters") {
      val r1 = createValueRaster(10, 1)
      val r2 = createValueRaster(10, 2)
      val r3 = createValueRaster(10, 3)
      val r0xF = createValueRaster(10, 15)

      val s1 = Seq(r1, r3)
      val s2 = Seq(r2, r3)
      val result1 = r0xF & s1
      val result2 = r0xF & s2
      for (y <- 0 until 10) {
        for (x <- 0 until 10) {
          result1.get(x,y) should be (1)
          result2.get(x,y) should be (2)
        }
      }
    }
  }
}
