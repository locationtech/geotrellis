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

class MultiplySpec extends FunSpec 
                      with Matchers 
                      with TestEngine 
                      with TileBuilders {
  describe("Multiply") {
    it("multiplys a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r * 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 5)
        }
      }
    }

    it("multiplys a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r * 3
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 3.0)
        }
      }
    }

    it("multiplys a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r * 5.1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) * 5.1).toInt)
        }
      }
    }

    it("multiplys a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r * .3
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 0.3)
        }
      }
    }

    it("multiplys an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = r * r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (math.pow(r.get(col,row),2.0))
        }
      }
    }
    
    it("multiplys a double raster to itself") {
      val r = probabilityRaster
      val result = r * r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (math.pow(r.getDouble(col,row), 2.0))
        }
      }
    }
  }
}
