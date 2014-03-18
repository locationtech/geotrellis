/**************************************************************************
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
 **************************************************************************/

package geotrellis.raster.op.local

import geotrellis._
import geotrellis.source._
import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class TanhSpec extends FunSpec
                with ShouldMatchers
                with TestServer
                with RasterBuilders {
  describe("Tanh") {
    it("finds the tanh of a double raster") {
      val rasterData = Array (
        0.0,  1.0/6,  1.0/3,    1.0/2,  2.0/3,  5.0/6,
        1.0,  7.0/6,  4.0/3,    3.0/2,  5.0/3, 11.0/6,
        2.0, 13.0/6,  7.0/3,    5.0/2,  8.0/3, 17.0/6,

       -0.0, -1.0/6, -1.0/3,   -1.0/2, -2.0/3, -5.0/6,
       -1.0, -7.0/6, -4.0/3,   -3.0/2, -5.0/3,-11.0/6,
       -2.0,-13.0/6, -7.0/3,    Double.PositiveInfinity,  Double.NegativeInfinity, Double.NaN
      ).map(_*math.Pi)
      val expected = rasterData.map(math.tanh(_))
      val rs = createRasterSource(rasterData, 3, 3, 2, 2)
      run(rs.localTanh) match {
        case Complete(result, success) =>
          for (y <- 0 until 6) {
            for (x <- 0 until 6) {
              val theTanh = result.getDouble(x, y)
              val epsilon = math.ulp(theTanh)
              val theExpected = expected(y*6 + x)
              if (!(x == 5 && y == 5)) {
                theTanh should be (theExpected plusOrMinus epsilon)
              } else {
                theTanh.isNaN should be (true)
              }
            }
          }
        case Error(msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
    it("finds the tanh of an int raster") {
      val rasterData = Array (
        0, 1,   2, 3,
        4, 5,   6, 7,

       -4,-5,  -6,-7,
       -1,-2,  -3,NODATA
      )
      val expected = rasterData.map(math.tanh(_))
                               .toList
                               .init
      val rs = createRasterSource(rasterData, 2, 2, 2, 2)
      run(rs.localTanh) match {
        case Complete (result, success) =>
          for (y <- 0 until 4) {
            for (x <- 0 until 4) {
              val isLastValue = (x == 3 && y == 3)
              if (!isLastValue) {
                val theResult = result.getDouble(x, y)
                val expectedResult = expected(y*4 + x)
                val epsilon = math.ulp(theResult)
                theResult should be (expectedResult plusOrMinus epsilon)
              } else {
                result.getDouble(x, y).isNaN should be (true)
              }
            }
          }
        case Error (msg, failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
  describe("Tanh on Raster") {
    it("finds the tanh of a double raster") {
      val rasterData = Array (
        0.0,  1.0/6,  1.0/3,    1.0/2,  2.0/3,  5.0/6,
        1.0,  7.0/6,  4.0/3,    3.0/2,  5.0/3, 11.0/6,
        2.0, 13.0/6,  7.0/3,    5.0/2,  8.0/3, 17.0/6,

       -0.0, -1.0/6, -1.0/3,   -1.0/2, -2.0/3, -5.0/6,
       -1.0, -7.0/6, -4.0/3,   -3.0/2, -5.0/3,-11.0/6,
       -2.0,-13.0/6, -7.0/3,    Double.PositiveInfinity,  Double.NegativeInfinity, Double.NaN
      ).map(_*math.Pi)
      val expected = rasterData.map(math.tanh(_))
      val r = createRaster(rasterData, 6, 6)
      val result = r.localTanh
      for (y <- 0 until 6) {
        for (x <- 0 until 6) {
          val theTanh = result.getDouble(x, y)
          val epsilon = math.ulp(theTanh)
          val theExpected = expected(y*6 + x)
          if (!(x == 5 && y == 5)) {
            theTanh should be (theExpected plusOrMinus epsilon)
          } else {
            theTanh.isNaN should be (true)
          }
        }
      }
    }
    it("finds the tanh of an int raster") {
      val rasterData = Array (
        0, 1,   2, 3,
        4, 5,   6, 7,

       -4,-5,  -6,-7,
       -1,-2,  -3,NODATA
      )
      val expected = rasterData.map(math.tanh(_))
                               .toList
                               .init
      val r = createRaster(rasterData, 4, 4)
      val result = r.localTanh
      for (y <- 0 until 4) {
        for (x <- 0 until 4) {
          val isLastValue = (x == 3 && y == 3)
          if (!isLastValue) {
            val theResult = result.getDouble(x, y)
            val expectedResult = expected(y*4 + x)
            val epsilon = math.ulp(theResult)
            theResult should be (expectedResult plusOrMinus epsilon)
          } else {
            result.getDouble(x, y).isNaN should be (true)
          }
        }
      }
    }
  }
}
