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

class NegateSpec extends FunSpec 
                    with Matchers 
                    with RasterMatchers 
                    with TileBuilders {
  describe("Negate") {
    it("negates an integer raster") {
      val r = positiveIntegerRaster
      val result = -r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-r.get(col,row))
        }
      }
    }

    it("negates a double raster") {
      val r = probabilityRaster
      val result = -r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-r.getDouble(col,row))
        }
      }
    }
  }
}
