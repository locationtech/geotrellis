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

class DivideSpec extends FunSpec 
                    with Matchers 
                    with TestEngine 
                    with TileBuilders {
  describe("Divide") {
    it("divides a constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = r / 5
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) / 5)
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = r / 3
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 3)
        }
      }
    }

    it("divides a constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = -10 /: r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-10 / r.get(col,row))
        }
      }
    }

    it("divides a constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = -3 /: r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.0 / r.getDouble(col,row))
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from right hand side") {
      val r = positiveIntegerRaster
      val result = r / 5.1
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ((r.get(col,row) / 5.1).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from right hand side") {
      val r = probabilityRaster
      val result = r / .3
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (r.getDouble(col,row) / 0.3)
        }
      }
    }

    it("divides a double constant value to each cell of an int valued raster, from left hand side") {
      val r = positiveIntegerRaster
      val result = -10.7 /: r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be ( (-10.7 / r.get(col,row)).toInt)
        }
      }
    }

    it("divides a double constant value to each cell of an double valued raster, from left hand side") {
      val r = probabilityRaster
      val result = -3.3 /: r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-3.3 / r.getDouble(col,row))
        }
      }
    }

    it("divides an integer raster to itself") {
      val r = positiveIntegerRaster
      val result = r / r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (1)
        }
      }
    }
    
    it("divides a double raster to itself") {
      val r = probabilityRaster
      val result = r / r
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (1.0)
        }
      }
    }
  }
}
