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

class LessOrEqualSpec extends FunSpec 
                         with Matchers 
                         with TestEngine 
                         with TileBuilders {
  describe("LessOrEqual") {
    it("checks int valued raster against int constant") {
      val r = positiveIntegerRaster
      val result = r <= 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val z = r.get(col, row)
          val rz = result.get(col, row)
          if(z <= 5) rz should be (1)
          else rz should be (0)
        }
      }
    }

    it("checks int valued raster against double constant") {
      val r = probabilityRaster.map(_ * 100).convert(TypeInt)
      val result = r <= 69.0
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val z = r.get(col, row)
          val rz = result.get(col, row)
          if(z <= 69) rz should be (1)
          else rz should be (0)
        }
      }
    }

    it("checks double valued raster against int constant") {
      val r = positiveIntegerRaster.convert(TypeDouble).mapDouble(_.toDouble)
      val result = r <= 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val z = r.getDouble(col, row)
          val rz = result.get(col, row)
          if(z <= 5.0) rz should be (1)
          else rz should be (0)
        }
      }
    }

    it("checks double valued raster against double constant") {
      val r = probabilityRaster
      val result = r <=0.69
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val z = r.getDouble(col, row)
          val rz = result.getDouble(col, row)
          if(z <= 0.69) rz should be (1)
          else rz should be (0)
        }
      }
    }

    it("checks an integer raster against itself") {
      val r = positiveIntegerRaster
      val result = r <= r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col, row) should be (1)
        }
      }
    }

    it("checks an integer raster against a different raster") {
      val r = positiveIntegerRaster
      val r2 = positiveIntegerRaster.map(_ * 2)
      val result = r <= r2
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col, row) should be (1)
        }
      }

      val result2 = r2 <= r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result2.get(col, row) should be (0)
        }
      }
    }

    it("checks a double raster against itself") {
      val r = probabilityRaster
      val result = r <= r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col, row) should be (1)
        }
      }
    }

    it("checks a double raster against a different raster") {
      val r = probabilityRaster
      val r2 = positiveIntegerRaster.mapDouble(_ * 2.3)
      val result = r <= r2
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col, row) should be (1)
        }
      }
      val result2 = r2 <= r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result2.get(col, row) should be (0)
        }
      }
    }
  }
}
