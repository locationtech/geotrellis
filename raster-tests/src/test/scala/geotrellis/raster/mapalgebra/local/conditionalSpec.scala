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

import scala.math.min

import geotrellis.raster.testkit._
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ConditionalSpec extends AnyFunSpec
                         with Matchers 
                         with RasterMatchers 
                         with TileBuilders {
  describe("IfCell") {
    it("should work with integers") {
      val r = positiveIntegerRaster
      val result = r.localIf({z:Int => z > 6}, 6)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          result.get(col,row) should be (min(r.get(col,row),6))
        }
      }
    }

    it("should work with doubles") {
      val r = probabilityRaster
      val result = r.localIf({z:Double => z > 0.5}, 1.0)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z = r.getDouble(col,row)
          result.getDouble(col,row) should be (if (z > 0.5) { 1.0 } else { z })
        }
      }
    }

    it("should work with integer function on DoubleConstantNoDataCellType raster for NoData values") {
      val r = probabilityNoDataRaster
      val result = r.localIf({z:Int => isNoData(z)}, -1000)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
        }
      }
    }

    it("should work with double function on IntConstantNoDataCellType raster for NoData values") {
      val r = positiveIntegerNoDataRaster
      val result = r.localIf({z:Double => isNoData(z)}, -1000.0)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
        }
      }
    }

    it("should work with integers with else value") {
      val r = positiveIntegerRaster
      val result = r.localIf({z:Int => z > 6}, 6, 2)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          result.get(col,row) should be (if (r.get(col,row) > 6) { 6 } else { 2 })
        }
      }
    }

    it("should work with doubles with else values") {
      val r = probabilityRaster
      val result = r.localIf({z:Double => z < .5}, 0.01, .99)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z = r.getDouble(col,row)
          result.getDouble(col,row) should be (if (z < 0.5) { 0.01 } else { .99 })
        }
      }
    }

    it("should work with integer function on DoubleConstantNoDataCellType raster for NoData values with else value") {
      val r = probabilityNoDataRaster
      val result = r.localIf({z:Int => isNoData(z)}, -1000, 2000)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
          else { result.getDouble(col,row) should be (2000.0) }
        }
      }
    }

    it("should work with double function on IntConstantNoDataCellType raster for NoData values withe else value") {
      val r = positiveIntegerNoDataRaster
      val result = r.localIf({z:Double => isNoData(z)}, -1000.0, 2000.0)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
          else { result.get(col,row) should be (2000) }
        }
      }
    }

    it("should work with integers with two rasters") {
      val r1 = createTile(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = r1.localIf(r2, {(z1:Int,z2:Int) => z1 > z2}, 80)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.get(col,row)
          val z2 = r2.get(col,row)
          val expected = if (z1 > z2) { 80 } else { z1 }
          result.get(col,row) should be (expected)
        }
      }
    }

    it("should work with doubles with two rasters") {
      val r1 = createTile(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = r1.localIf(r2, {(z1:Double,z2:Double) => z1 > z2}, -0.5)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.getDouble(col,row)
          val z2 = r2.getDouble(col,row)
          val expected = if (z1 > z2) { -0.5 } else { z1 }
          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("should work with integer function on two DoubleConstantNoDataCellType rasters for NoData values") {
      val r1 = createTile(Array( -.1,  Double.NaN, -.13, .5,
                                   -.12, .7,  -.3, Double.NaN,
                                   -.8 , Double.NaN, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, Double.NaN,
                                   .12, Double.NaN,  .3, -.2,
                                   .8 , -.6, .12, Double.NaN), 4,3)
      val result = r1.localIf(r2,{(z1:Int,z2:Int) => isNoData(z1) || isNoData(z2) }, -1000)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
        }
      }
    }

    it("should work with double function on two IntConstantNoDataCellType rasters for NoData values") {
      val r1 = createTile(Array( -1,  NODATA, -13, 5,
                                   -12, 7,  -3, NODATA,
                                   -8 , NODATA, -12, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, NODATA,
                                   12, NODATA,  3, -2,
                                   8 , -6, 12, NODATA), 4,3)
      val result = r1.localIf(r2,{ (z1:Double,z2:Double) =>
        isNoData(z1) || isNoData(z2)
      }, -1000.0)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
        }
      }
    }

    it("should work with integers on two rasters and else value") {
      val r1 = createTile(Array( -1,  2, -13, 5,
                                   -12, 7,  -3, 2,
                                   -8 , 6, -12, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, -5,
                                   12, -7,  3, -2,
                                   8 , -6, 12, -7), 4,3)
      val result = r1.localIf(r2, {(z1:Int,z2:Int) => z1 > z2}, 80,-20)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.get(col,row)
          val z2 = r2.get(col,row)
          val expected = if (z1 > z2) { 80 } else { -20 }
          result.get(col,row) should be (expected)
        }
      }
    }

    it("should work with doubles with two rasters and else value") {
      val r1 = createTile(Array( -.1,  .25, -.13, .5,
                                   -.12, .7,  -.3, .2,
                                   -.8 , .6, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, -.5,
                                   .12, -.7,  .3, -.2,
                                   .8 , -.6, .12, -.7), 4,3)
      val result = r1.localIf(r2, {(z1:Double,z2:Double) => z1 > z2}, 1.1,0.7)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          val z1 = r1.getDouble(col,row)
          val z2 = r2.getDouble(col,row)
          val expected = if (z1 > z2) { 1.1 } else { 0.7 }
          result.getDouble(col,row) should be (expected)
        }
      }
    }

    it("should work with integer function on two DoubleConstantNoDataCellType rasters for NoData values and else value") {
      val r1 = createTile(Array( -.1,  Double.NaN, -.13, .5,
                                   -.12, .7,  -.3, Double.NaN,
                                   -.8 , Double.NaN, -.12, .7), 4,3)
      val r2 = createTile(Array( .1,  .2, .13, Double.NaN,
                                   .12, Double.NaN,  .3, -.2,
                                   .8 , -.6, .12, Double.NaN), 4,3)
      val result = r1.localIf(r2,{(z1:Int,z2:Int) => isNoData(z1) || isNoData(z2) }, -1000,2000)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.getDouble(col,row) should be (-1000.0)
          else { result.getDouble(col,row) should be (2000.0) }
        }
      }
    }

    it("should work with double function on two IntConstantNoDataCellType rasters for NoData values and else value") {
      val r1 = createTile(Array( -1,  NODATA, -13, 5,
                                   -12, 7,  -3, NODATA,
                                   -8 , NODATA, -12, 7), 4,3)
      val r2 = createTile(Array( 1,  -2, 13, NODATA,
                                   12, NODATA,  3, -2,
                                   8 , -6, 12, NODATA), 4,3)
      val result = r1.localIf(r2,{ (z1:Double,z2:Double) =>
        isNoData(z1) || isNoData(z2)
      }, -1000.0,2000.0)
      for(col <- 0 until result.cols) {
        for(row <- 0 until result.rows) {
          if(col % 2 == 1) result.get(col,row) should be (-1000)
          else { result.get(col,row) should be (2000) }
        }
      }
    }
  }
}
