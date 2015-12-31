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

class AbsSpec extends FunSpec
                 with Matchers
                 with RasterMatchers
                 with TileBuilders {
  describe("Abs") {
    it("takes the absolute value of each cell of an int raster") {
      val rasterData = Array(
         1,-1, 1,  -1, 1,-1,  1,-1, NODATA,
        -1, 1,-1,   1,-1, 1, -1, 1,-1,

         1,-1, 1,  -1, 1,-1,  1,-1, 1,
        -1, 1,-1,   1,-1, 1, -1, 1,-1
      )
      val r = createTile(rasterData, 9, 4)
      val result = r.localAbs()

      for (y <- 0 until 4) {
        for (x <- 0 until 9) {
          if (x == 8 && y == 0)
            result.get(x, y) should be (NODATA)
          else
            result.get(x, y) should be (1)
        }
      }
    }

    it("takes the absolute value of each cell of a double raster") {
      val rasterData = Array(
         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, 2.6,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6,

         2.6,-2.6, 2.6,  -2.6, 2.6,-2.6,  2.6,-2.6, Double.NaN,
        -2.6, 2.6,-2.6,   2.6,-2.6, 2.6, -2.6, 2.6,-2.6
      )
      val r = createTile(rasterData, 9, 4)
      val result = r.localAbs()
      for (y <- 0 until 4) {
        for (x <- 0 until 9) {
          if (x == 8 && y == 2)
            result.getDouble(x, y).isNaN should be (true)
          else
            result.getDouble(x, y) should be (2.6)
        }
      }
    }
  }
}
