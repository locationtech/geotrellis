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

class MinSpec extends FunSpec 
                 with Matchers 
                 with TestEngine 
                 with TileBuilders {
  describe("Min") {    
    it("mins a constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r.localMin(50)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.get(col,row) < 50) { r.get(col,row) }
                         else { 50 }
          
          result.get(col,row) should be (expected)
        }
      }
    }

    it("produces NODATA for NODATA cells of an int valued raster") {
      val r = positiveIntegerNoDataRaster
      val result = r.localMin(50)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) {
            result.get(col,row) should be (NODATA)
          }
        }
      }
    }

    it("mins a constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r.localMin(1)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should not be (1.0)
        }
      }
    }

    it("produces Double.NaN for Double.NaN cells of an Double valued raster") {
      val r = probabilityNoDataRaster
      val result = r.localMin(-1)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) {
            isNoData(result.getDouble(col,row)) should be (true)
          }
        }
      }
    }
    
    it("mins a double constant value to each cell of an int valued raster") {
      val r = positiveIntegerRaster
      val result = r.localMin(40.1)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.get(col,row) < 40.1) { r.get(col,row) }
                         else { 40 }

          result.get(col,row) should be (expected)
        }
      }
    }

    it("takes NODATA for NODATA cells of an int valued raster and double constant") {
      val r = positiveIntegerNoDataRaster
      val result = r.localMin(52.4)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) {
            result.get(col,row) should be (NODATA)
          }
        }
      }
    }

    it("mins a double constant value to each cell of an double valued raster") {
      val r = probabilityRaster
      val result = r.localMin(.3)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          val expected = if(r.getDouble(col,row) < .3) { r.getDouble(col,row) }
                         else { .3 }

          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("prodcues NaN for Double.NaN cells of an Double valued raster") {
      val r = probabilityNoDataRaster
      val result = r.localMin(-.04)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          if(col % 2 == 1) {
            isNoData(result.getDouble(col,row)) should be (true)
          }
        }
      }
    }

    it("sets all data to NODATA if constant is NODATA") {
      val r1 = positiveIntegerNoDataRaster
      val r2 = probabilityNoDataRaster
      assertEqual(r1.localMin(NODATA), r1.map(z=>NODATA))
      assertEqual(r2.localMin(NODATA), r2.map(z=>NODATA))
    }

    it("sets all data to NaN if constant is Double.NaN") {
      val r1 = positiveIntegerNoDataRaster
      val r2 = probabilityNoDataRaster
      assertEqual(r1.localMin(Double.NaN), r1.mapDouble(z=>Double.NaN))
      assertEqual(r2.localMin(Double.NaN), r2.mapDouble(z=>Double.NaN))
    }

    it("mins two integer rasters") {
      val r1 = createTile(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = r1.localMin(r2)
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          if(col % 2 != 1) {
            result.get(col,row) should be (r1.get(col,row))
          } else {
            result.get(col,row) should be (r2.get(col,row))
          }
        }
      }
    }

    it("mins two double rasters") {
      val r1 = createTile(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = r1.localMin(r2)
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          if(col % 2 != 1) {
            result.getDouble(col,row) should be (r1.getDouble(col,row))
          } else {
            result.getDouble(col,row) should be (r2.getDouble(col,row))
          }
        }
      }
    }    

    it("mins two integer rasters with NODATA") {
      val r1 = createTile(Array( NODATA,  -1, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, NODATA, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, NODATA, -7), 4,3)
      val result = r1.localMin(r2)
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          val z1 = r1.get(col,row)
          val z2 = r2.get(col,row)

          if(isNoData(z1) || isNoData(z2)) {
            result.get(col,row) should be (NODATA)
          } else if(col % 2 == 0) {
            result.get(col,row) should be (z1)
          } else {
            result.get(col,row) should be (r2.get(col,row))
          }
        }
      }
    }

    it("mins two double rasters with Double.NaN values") {
      val r1 = createTile(Array(  Double.NaN, .25, -.13, NODATA.toDouble,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, NODATA.toDouble - 1,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = r1.localMin(r2)
      for(col <- 0 until 4) {
        for(row <- 0 until 3) {
          val z1 = r1.getDouble(col,row)
          val z2 = r2.getDouble(col,row)
          val zr = result.getDouble(col,row)

          if(isNoData(z1) || isNoData(z2)) {
            withClue(s"Z1: $z1  Z2: $z2  R: $zr") { isNoData(zr) should be (true) }
          } else if(col % 2 == 0) {
            zr should be (z1)
          } else {
            zr should be (z2)
          }
        }
      }
    }    
  }
}
