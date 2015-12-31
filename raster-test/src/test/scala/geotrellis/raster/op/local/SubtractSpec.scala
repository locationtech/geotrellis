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
import geotrellis.vector.Extent

import org.scalatest._
import geotrellis.testkit._

class SubtractSpec extends FunSpec 
                 with Matchers 
                 with RasterMatchers 
                 with TileBuilders {
  describe("Subtract") {
    it("subtracts a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r - 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) - 5)
        }
      }
    }

    it("subtracts a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r - 1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 1.0)
        }
      }
    }

    it("subtracts a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r - 5.7
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (r.get(col,row) - 5.7).toInt)
        }
      }
    }

    it("subtracts a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r - 0.1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) - 0.1)
        }
      }
    }

    it("subtracts an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = r - r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (0)
        }
      }
    }
    
    it("subtracts a double raster to itself") {
      val r = probabilityRaster
      val result = r - r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (0.0)
        }
      }
    }

    it("should subtract 63 and 17 correctly") {
      val cols = 100
      val rows = 100

      val e = Extent(0.0, 0.0, 100.0, 100.0)
      val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)

      def makeData(c:Int) = Array.fill(cols * rows)(c)
      def makeRaster(c:Int) = ArrayTile(makeData(c), cols, rows)

      val r63 = makeRaster(63)
      val r46 = makeRaster(46)
      val r17 = makeRaster(17)

      val r = r63 - r17
      r.get(0, 0) should be (r46.get(0, 0))
    }
  }
}
