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

class SqrtSpec extends AnyFunSpec
                  with Matchers 
                  with RasterMatchers 
                  with TileBuilders {
  describe("Sqrt") {
    it("takes the square root of an integer raster") {
      val r = positiveIntegerRaster
      val result = r.localSqrt()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.sqrt(r.get(col,row)).toInt)
        }
      }
    }

    it("takes the square root of a double raster") {
      val r = probabilityRaster
      val result = r.localSqrt()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.sqrt(r.getDouble(col,row)))
        }
      }
    }
  }
}
