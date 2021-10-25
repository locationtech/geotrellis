/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

import geotrellis.raster.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class RoundSpec extends AnyFunSpec
                   with Matchers 
                   with RasterMatchers 
                   with TileBuilders {
  describe("Round") {    
    it("correctly rounds a Int Raster") {
      val r = createTile(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1), 9, 4)
      val result = r.localRound()
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if (row == 0 && col == 0) {
            result.get(col,row) should be (NODATA)
          }
          else {
            // Rounding on an Int is a no-op
            result.get(col,row) should be (r.get(col,row))
          }
        }
      }
    }
    it("correctly rounds a Double Raster") {
      val r = createTile(
        Array( Double.NaN,1.0,1.0, 1.0,1.0,1.0, 1.0,1.0,1.0,
               1.2,1.2,1.2, 1.2,1.2,1.2, 1.2,1.2,1.2,

               1.5,1.5,1.5, 1.5,1.5,1.5, 1.5,1.5,1.5,
               1.8,1.8,1.8, 1.8,1.8,1.8, 1.8,1.8,1.8), 9, 4)
      val result = r.localRound()
      for(row <- 0 until 4) {
        for(col <- 0 until 9) {
          if (row == 0 && col == 0) {
            result.getDouble(col,row).isNaN should be (true)
          }
          else {
            result.getDouble(col,row) should be (math.round(r.getDouble(col,row)))
          }
        }
      }
    }
  }
}
