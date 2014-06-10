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

class VarietySpec extends FunSpec 
                     with Matchers 
                     with TestServer 
                     with RasterBuilders {
  describe("Variety") {
    it("computes variety") { 
      val n = NODATA
      val r1 = createRaster(
        Array( n, n, n, n, n,
               n, n, n, n, n,
               n, n, n, n, n,
               n, n, n, n, n), 
        5,4
      )

      val r2 = createRaster(
        Array( n, 1, n, n, n,
               n, n, 1, n, n,
               n, n, n, 1, n,
               n, n, n, n, 1), 
        5,4
      )

      val r3 = createRaster(
        Array( n, 2, n, n, n,
               n, n, 2, n, n,
               n, n, n, 2, n,
               n, n, n, n, 1), 
        5,4
      )

      val r4 = createRaster(
        Array( n, 3, n, n, n,
               n, n, 3, n, n,
               n, n, n, 2, n,
               n, n, n, n, 1), 
        5,4
      )

      val r5 = createRaster(
        Array( n, 4, n, n, n,
               n, n, 3, n, n,
               n, n, n, 2, n,
               n, n, n, n, 1), 
        5,4
      )

      val variety = get(Variety(r1,r2,r3,r4,r5))
      for(col <- 0 until 5) {
        for(row <- 0 until 4) {
          if(col== row + 1) {
            col match {
              case 1 => variety.get(col,row) should be (4)
              case 2 => variety.get(col,row) should be (3)
              case 3 => variety.get(col,row) should be (2)
              case 4 => variety.get(col,row) should be (1)
            }
          } else {
            variety.get(col,row) should be (NODATA)
          }
        }
      }
    }

    it("computes variety on raster sources") { 
      val n = NODATA
      val r1 = createRasterSource(
        Array( n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n), 
        2,2,3,2
      )

      val r2 = createRasterSource(
        Array( n, 1, n, n, n, n,
               n, n, 1, n, n, n,
               n, n, n, 1, n, n,
               n, n, n, n, 1, n), 
        2,2,3,2
      )

      val r3 = createRasterSource(
        Array( n, 2, n, n, n, n,
               n, n, 2, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),  
        2,2,3,2
      )

      val r4 = createRasterSource(
        Array( n, 3, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n), 
        2,2,3,2
      )

      val r5 = createRasterSource(
        Array( n, 4, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n), 
        2,2,3,2
      )

      run(r1.localVariety(r2,r3,r4,r5)) match {
        case Complete(result,success) =>
          for(col <- 0 until 6) {
            for(row <- 0 until 4) {
              if(col== row + 1) {
                col match {
                  case 1 => result.get(col,row) should be (4)
                  case 2 => result.get(col,row) should be (3)
                  case 3 => result.get(col,row) should be (2)
                  case 4 => result.get(col,row) should be (1)
                  case 5 => result.get(col,row) should be (NODATA)
                }
              } else {
                result.get(col,row) should be (NODATA)
              }
            }
          }

        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("computes variety on raster using local method") {
      val n = NODATA
      val r2 = createRaster(
        Array( n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n),
        6,4
      )

      val r1 = createRaster(
        Array( n, 1, n, n, n, n,
               n, n, 1, n, n, n,
               n, n, n, 1, n, n,
               n, n, n, n, 1, n),
        6,4
      )

      val r3 = createRaster(
        Array( n, 2, n, n, n, n,
               n, n, 2, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6,4
      )

      val r4 = createRaster(
        Array( n, 3, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6,4
      )

      val r5 = createRaster(
        Array( n, 4, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6,4
      )

      val result = r1.localVariety(r2,r3,r4,r5)

      for(col <- 0 until 6) {
        for(row <- 0 until 4) {
          if(col== row + 1) {
            col match {
              case 1 => result.get(col,row) should be (4)
              case 2 => result.get(col,row) should be (3)
              case 3 => result.get(col,row) should be (2)
              case 4 => result.get(col,row) should be (1)
              case 5 => result.get(col,row) should be (NODATA)
            }
          } else {
            result.get(col,row) should be (NODATA)
          }
        }
      }
    }
  }
}
