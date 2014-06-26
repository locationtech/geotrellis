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

import org.scalatest._

import geotrellis.testkit._

class MeanSpec extends FunSpec 
                  with Matchers 
                  with TestEngine 
                  with TileBuilders {
  describe("Mean") {
    it("takes mean on rasters of all one value") {
      val r1 = createTile(Array.fill(7*8)(1), 7, 8)
      val r2 = createTile(Array.fill(7*8)(5), 7, 8)
      val r3 = createTile(Array.fill(7*8)(10), 7, 8)

      assertEqual(Mean(r1,r2,r3),Array.fill(7*8)((1+5+10)/3))
    }

    it("takes mean on rasters of varying values") {
      val n = NODATA
      val r1 = createTile(Array(
        n, 1, n, 1, n, 1,
        2, n, 2, n, 2, n,
        n, 3, n, 3, n, 3,
        4, n, 4, n, 4, n,
        n, 5, n, 5, n, 5), 6,5)

      val r2 = createTile(Array(
        1, n, n, n, n, 10,
        1, n, n, n, n, 9,
        1, n, n, n, n, 8,
        1, n, n, n, n, 7,
        1, n, n, n, n, 6), 6,5)

      val r3 = createTile(Array(
        n, 8, n, 9, n, 1,
        n, n, n, 7, 2, n,
        n, 8, n, 5, 2, n,
        n, 8, n, 3, 4, n,
        n, 8, n, 1, n, n), 6,5)

      val expected = Array(
              1, (8+1)/2, n, (9+1)/2,       n, (1+10+1)/3,
        (2+1)/2,       n, 2,       7, (2+2)/2,          9,
              1, (8+3)/2, n, (3+5)/2,       2,    (8+3)/2,
        (4+1)/2,       8, 4,       3, (4+4)/2,          7,
              1, (8+5)/2, n, (5+1)/2,       n,    (6+5)/2)

      assertEqual(Mean(r1,r2,r3),expected)
    }

    it("takes mean on raster sources of varying values") {
      val n = NODATA
      val rs1 = createRasterSource(Array(
        n, 1, n,   1, n, 1,
        2, n, 2,   n, 2, n,

        n, 3, n,   3, n, 3,
        4, n, 4,   n, 4, n), 2, 2, 3,2)

      val rs2 = createRasterSource(Array(
        1, n, n,   n, n, 10,
        1, n, n,   n, n, 9,

        1, n, n,   n, n, 8,
        1, n, n,   n, n, 7 ), 2, 2, 3, 2)

      val rs3 = createRasterSource(Array(
        n, 8, n,   9, n, 1,
        n, n, n,   7, 2, n,

        n, 8, n,   5, 2, n,
        n, 8, n,   3, 4, n), 2, 2, 3, 2)

      val expected = Array(
              1, (8+1)/2, n, (9+1)/2,       n, (1+10+1)/3,
        (2+1)/2,       n, 2,       7, (2+2)/2,          9,
              1, (8+3)/2, n, (3+5)/2,       2,    (8+3)/2,
        (4+1)/2,       8, 4,       3, (4+4)/2,          7
      )

      run(rs1.localMean(rs2, rs3)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 6) {
              result.get(col,row) should be (expected(row*6 + col))
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("takes mean on double raster sources of varying values") {
      val rs1 = createRasterSource(Array(
        NaN, 1.0, NaN,   1.0, NaN, 1.0,
        2.0, NaN, 2.0,   NaN, 2.0, NaN,

        NaN, 3.5, NaN,   3.5, NaN, 3.5,
        4.1, NaN, 4.1,   NaN, 4.1, NaN), 
        2, 2, 3,2)

      val rs2 = createRasterSource(Array(
        1.0, NaN, NaN,   NaN, NaN, 10.4,
        1.0, NaN, NaN,   NaN, NaN, 9.4,

        1.0, NaN, NaN,   NaN, NaN, 8.3,
        1.0, NaN, NaN,   NaN, NaN, 7.3 ), 2, 2, 3, 2)

      val rs3 = createRasterSource(Array(
        NaN, 8.3, NaN,   9.1, NaN, 1.2,
        NaN, NaN, NaN,   7.1, 2.2, NaN,

        NaN, 8.3, NaN,   5.1, 2.2, NaN,
        NaN, 8.3, NaN,   3.1, 4.2, NaN), 2, 2, 3, 2)

      val expected = Array(
              1.0, (8.3+1.0)/2, NaN, (9.1+1.0)/2,       NaN, (1.2+10.4+1.0)/3,
        (2.0+1.0)/2,       NaN, 2.0,         7.1, (2.2+2.0)/2,          9.4,
              1.0, (8.3+3.5)/2, NaN, (3.5+5.1)/2,       2.2,    (8.3+3.5)/2,
        (4.1+1.0)/2,       8.3, 4.1,       3.1, (4.2+4.1)/2,          7.3
      )

      run(rs1.localMean(rs2, rs3)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 6) {
              if(isNoData(expected(row*6+col))) { isNoData(result.getDouble(col,row)) should be (true) }
              else { result.getDouble(col,row) should be (expected(row*6 + col)) }
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("takes mean on rasters of varying values using local method") {
      val n = NODATA
      val rs1 = createTile(Array(
        n, 1, n,   1, n, 1,
        2, n, 2,   n, 2, n,

        n, 3, n,   3, n, 3,
        4, n, 4,   n, 4, n), 6, 4)

      val rs2 = createTile(Array(
        1, n, n,   n, n, 10,
        1, n, n,   n, n, 9,

        1, n, n,   n, n, 8,
        1, n, n,   n, n, 7 ), 6, 4)

      val rs3 = createTile(Array(
        n, 8, n,   9, n, 1,
        n, n, n,   7, 2, n,

        n, 8, n,   5, 2, n,
        n, 8, n,   3, 4, n), 6, 4)

      val expected = Array(
              1, (8+1)/2, n, (9+1)/2,       n, (1+10+1)/3,
        (2+1)/2,       n, 2,       7, (2+2)/2,          9,
              1, (8+3)/2, n, (3+5)/2,       2,    (8+3)/2,
        (4+1)/2,       8, 4,       3, (4+4)/2,          7
      )

      val result:Raster = rs1.localMean(rs2, rs3)
      for(row <- 0 until 4) {
        for(col <- 0 until 6) {
          result.get(col,row) should be (expected(row*6 + col))
        }
      }
    }

    it("takes mean on double rasters of varying values using local method") {
      val rs1 = createTile(Array(
        NaN, 1.0, NaN,   1.0, NaN, 1.0,
        2.0, NaN, 2.0,   NaN, 2.0, NaN,

        NaN, 3.5, NaN,   3.5, NaN, 3.5,
        4.1, NaN, 4.1,   NaN, 4.1, NaN), 
        6, 4)

      val rs2 = createTile(Array(
        1.0, NaN, NaN,   NaN, NaN, 10.4,
        1.0, NaN, NaN,   NaN, NaN, 9.4,

        1.0, NaN, NaN,   NaN, NaN, 8.3,
        1.0, NaN, NaN,   NaN, NaN, 7.3 ), 6, 4)

      val rs3 = createTile(Array(
        NaN, 8.3, NaN,   9.1, NaN, 1.2,
        NaN, NaN, NaN,   7.1, 2.2, NaN,

        NaN, 8.3, NaN,   5.1, 2.2, NaN,
        NaN, 8.3, NaN,   3.1, 4.2, NaN), 6, 4)

      val expected = Array(
              1.0, (8.3+1.0)/2, NaN, (9.1+1.0)/2,       NaN, (1.2+10.4+1.0)/3,
        (2.0+1.0)/2,       NaN, 2.0,         7.1, (2.2+2.0)/2,          9.4,
              1.0, (8.3+3.5)/2, NaN, (3.5+5.1)/2,       2.2,    (8.3+3.5)/2,
        (4.1+1.0)/2,       8.3, 4.1,       3.1, (4.2+4.1)/2,          7.3
      )

      val result = rs1.localMean(rs2, rs3)
      for(row <- 0 until 4) {
        for(col <- 0 until 6) {
          if(isNoData(expected(row*6+col))) {
            isNoData(result.getDouble(col,row)) should be (true)
          }
          else {
            result.getDouble(col,row) should be (expected(row*6 + col))
          }
        }
      }

    }
  }
}
