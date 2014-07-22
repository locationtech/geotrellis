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

import org.scalatest._

import geotrellis.testkit._

class GreaterOrEqualSpec extends FunSpec 
                            with Matchers 
                            with TestEngine 
                            with TileBuilders {
  describe("GreaterOrEqual") {
    it("Compares two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2")

      run(rs1 >= rs2) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until result.rows) {
            for(col <- 0 until result.cols) {
              result.get(col,row) should be (1)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("compares Greater or equal on a RasterSource and an int correctly") {
      val rs1 = RasterSource("quad_tiled")

      run(rs1 >= 5) match {
        case Complete(result,success) =>
          for(row <- 0 until result.rows) {
            for(col <- 0 until result.cols) {
              val cellResult = result.get(col,row)
              if (result.get(col,row) >= 5) cellResult should be (1)
              else cellResult should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("compares Greater or equal on an int and a RasterSource correctly") {
      val rs1 = RasterSource("quad_tiled")

      run(5 >=: rs1) match {
        case Complete(result,success) =>
          for(row <- 0 until result.rows) {
            for(col <- 0 until result.cols) {
              val cellResult = result.get(col,row)
              if (5 >= result.get(col,row)) cellResult should be (1)
              else cellResult should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("compares two tiled unequalRasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2") + 1

      run(rs1 >= rs2) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until result.rows) {
            for(col <- 0 until result.cols) {
              result.get(col,row) should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }

      run(rs2 >= rs1) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until result.rows) {
            for(col <- 0 until result.cols) {
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
}
