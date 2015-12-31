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

class UndefinedSpec extends FunSpec 
                       with Matchers 
                       with RasterMatchers 
                       with TileBuilders {
  describe("Undefined") {
    it("returns correct result for an integer raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.localUndefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.get(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }

    it("returns correct result for a double raster") {
      val r = probabilityNoDataRaster
      val result = r.localUndefined()
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(isNoData(r.getDouble(col,row))) result.get(col,row) should be (1)
          else result.get(col,row) should be (0)
        }
      }
    }
  }
}
