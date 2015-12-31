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

class CosSpec extends FunSpec
                 with Matchers
                 with RasterMatchers
                 with TileBuilders {
  describe("Cos") {
    it("finds the cosine of a double raster") {
      val rasterData = Array (
        0.0,  1.0/6,  1.0/3,    1.0/2,  2.0/3,  5.0/6,
        1.0,  7.0/6,  4.0/3,    3.0/2,  5.0/3, 11.0/6,
        2.0, 13.0/6,  7.0/3,    5.0/2,  8.0/3, 17.0/6,

       -0.0, -1.0/6, -1.0/3,   -1.0/2, -2.0/3, -5.0/6,
       -1.0, -7.0/6, -4.0/3,   -3.0/2, -5.0/3,-11.0/6,
       -2.0,-13.0/6, -7.0/3,    Double.PositiveInfinity,  Double.NegativeInfinity, Double.NaN
      ).map(_ * math.Pi)
      val expected = rasterData.map(math.cos(_))
      val rs = createTile(rasterData, 6, 6)
      val result = rs.localCos()
      for (y <- 0 until 6) {
        for (x <- 0 until 6) {
          val theCos = result.getDouble(x, y)
          val epsilon = math.ulp(theCos)
          val theExpected = expected(y*6 + x)
          if (x >= 3 && y == 5) {
            theCos.isNaN should be (true)
          } else {
            theCos should be (theExpected +- epsilon)
          }
        }
      }
}
    it("finds the cosine of an int raster") {
      val rasterData = Array (
        0, 1,   2, 3,
        4, 5,   6, 7,

       -4,-5,  -6,-7,
       -1,-2,  -3,NODATA
      )
      val expected = rasterData.map(math.cos(_))
                               .toList
                               .init
      val rs = createTile(rasterData, 4, 4)
      val result = rs.localCos()
      for (y <- 0 until 4) {
        for (x <- 0 until 4) {
          val isLastValue = (x == 3 && y == 3)
          if (!isLastValue) {
            val theResult = result.getDouble(x, y)
            val expectedResult = expected(y*4 + x)
            val epsilon = math.ulp(theResult)
            theResult should be (expectedResult +- epsilon)
          } else {
            result.getDouble(x, y).isNaN should be (true)
          }
        }
      }
    }
  }
}
