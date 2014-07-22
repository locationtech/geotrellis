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

class AddSpec extends FunSpec 
                 with Matchers 
                 with TestEngine 
                 with TileBuilders {
  describe("Add") {
    it("adds a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r + 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 5)
        }
      }
    }

    it("adds a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r + 1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) + 1.0)
        }
      }
    }

    it("adds a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r + 5.1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (r.get(col,row) + 5.1).toInt)
        }
      }
    }

    it("adds a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r + .3
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) + 0.3)
        }
      }
    }

    it("adds an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = r + r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) * 2)
        }
      }
    }

    it("adds a double raster to itself") {
      val r = probabilityRaster
      val result = r + r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) * 2.0)
        }
      }
    }

    it("adds three rasters correctly") {
      val r1 = ArrayTile(Array.fill(100)(3), 10, 10)
      val r2 = ArrayTile(Array.fill(100)(6), 10, 10)
      val r3 = ArrayTile(Array.fill(100)(9), 10, 10)

      assert(r1 + r2 === r3)
    }
  }
  
  describe("Add with sequences of rasters") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)

    def ri(n:Int) = ArrayTile(Array.fill(100)(n), 10, 10)
    def rd(n:Double) = ArrayTile(Array.fill(100)(n), 10 ,10)

    def addInts(ns:Int*) = (ns.map(n => ri(n))).localAdd
    def addDoubles(ns:Double*) = (ns.map(n => rd(n))).localAdd

    it("adds integers") {
      val a = 3
      val b = 6
      val c = 9
      val n = NODATA

      assertEqual(addInts(a, b),ri(c))
      assertEqual(addInts(n, b),ri(n))
      assertEqual(addInts(c, n),ri(n))
      assertEqual(addInts(n, n),ri(n))
    }

    it("adds doubles") {
      val a = 3000000000.0
      val b = 6000000000.0
      val c = 9000000000.0
      val x = a + a + b + b + c
      val n = Double.NaN

      assertEqual(addDoubles(a, b),rd(c))
      assertEqual(addDoubles(n, b),rd(n))
      assertEqual(addDoubles(c, n),rd(n))
      assertEqual(addDoubles(n, n),rd(n))

      assertEqual(addDoubles(a, a, b, b, c),rd(x))
    }
  }
}
