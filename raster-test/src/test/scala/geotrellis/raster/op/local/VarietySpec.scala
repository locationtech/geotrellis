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

import org.scalatest._

import geotrellis.testkit._

class VarietySpec extends FunSpec 
                     with Matchers 
                     with RasterMatchers 
                     with TileBuilders {
  describe("Variety") {
    it("computes variety") { 
      val n = NODATA
      val r2 = createTile(
        Array( n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n,
               n, n, n, n, n, n),
        6, 4
      )

      val r1 = createTile(
        Array( n, 1, n, n, n, n,
               n, n, 1, n, n, n,
               n, n, n, 1, n, n,
               n, n, n, n, 1, n),
        6, 4
      )

      val r3 = createTile(
        Array( n, 2, n, n, n, n,
               n, n, 2, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6, 4
      )

      val r4 = createTile(
        Array( n, 3, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6, 4
      )

      val r5 = createTile(
        Array( n, 4, n, n, n, n,
               n, n, 3, n, n, n,
               n, n, n, 2, n, n,
               n, n, n, n, 1, n),
        6, 4
      )

      val result = Seq(r1, r2, r3, r4, r5).localVariety

      for(col <- 0 until 6) {
        for(row <- 0 until 4) {
          if(col== row + 1) {
            col match {
              case 1 => result.get(col, row) should be (4)
              case 2 => result.get(col, row) should be (3)
              case 3 => result.get(col, row) should be (2)
              case 4 => result.get(col, row) should be (1)
              case 5 => result.get(col, row) should be (NODATA)
            }
          } else {
            result.get(col, row) should be (NODATA)
          }
        }
      }
    }
  }
}
