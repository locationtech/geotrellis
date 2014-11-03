/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class LocalSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with SharedSparkContext
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Local Operations") {
    ifCanRunSpark {
      val allOnes = AllOnesTestFile
      val increasing = IncreasingTestFile
      val everyOtherUndefined = EveryOtherUndefinedTestFile
      val everyOther0Point99Else1Point01 =
        EveryOther0Point99Else1Point01TestFile
      val everyOther1ElseMinus1 = EveryOther1ElseMinus1TestFile

      val cols = allOnes.metaData.tileLayout.totalCols

      it("should local mask two rasters") {
        val inc = increasing
        val ones = allOnes

        val res = ones.localMask(inc, 1, -1337)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) -1337 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(ones, res)
      }

      it("should local inverse mask two rasters") {
        val inc = increasing
        val ones = allOnes

        val res = ones.localInverseMask(inc, 1, -1337)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 1 else -1337
        )

        rastersShouldHaveSameIdsAndTileCount(ones, res)
      }

      it("should set all undefined values to 0 and the rest to one") {
        val everyOther = everyOtherUndefined

        val res = everyOther.localDefined

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(everyOther, res)
      }

      it("should set all defined values to 0 and the rest to one") {
        val everyOther = everyOtherUndefined

        val res = everyOther.localUndefined

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 0
        )

        rastersShouldHaveSameIdsAndTileCount(everyOther, res)
      }

      it("should square root all values in raster") {
        val inc = increasing

        val res = inc.localSqrt

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.sqrt(y * cols + x),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should root all values in raster") {
        val evo = everyOther1ElseMinus1

        val res = evo.localRound

        rasterShouldBe(
          res,
          (x: Int, y: Int) => 1
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should log all values in raster") {
        val inc = increasing

        val res = inc.localLog

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.log(y * cols + x),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should log base 10 all values in raster") {
        val inc = increasing

        val res = inc.localLog10

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.log10(y * cols + x),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should floor all values in raster") {
        val evo = everyOther1ElseMinus1

        val res = evo.localFloor

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should ceil all values in raster") {
        val evo = everyOther1ElseMinus1

        val res = evo.localCeil

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 2
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should negate all values in raster") {
        val inc = increasing

        val res = inc.localNegate

        rasterShouldBe(
          res,
          (x: Int, y: Int) => (y * cols + x) * -1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should negate with unary operator all values in raster") {
        val inc = increasing

        val res = -inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => (y * cols + x) * -1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should not all values in raster") {
        val inc = increasing

        val res = inc.localNot

        rasterShouldBe(
          res,
          (x: Int, y: Int) => ~(y * cols + x)
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should abs all values in raster") {
        val evo = everyOther1ElseMinus1

        val res = evo.localAbs

        rasterShouldBe(
          res,
          (x: Int, y: Int) => 1
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should arc cos all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localAcos

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.acos(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should arc sin all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localAsin

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.asin(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should arc tangent 2 all values in raster") {
        val evo = everyOther0Point99Else1Point01
        val evoOneMinusOne = everyOther1ElseMinus1

        val res = evo.localAtan2(evoOneMinusOne)

        rasterShouldBe(
          res,
          (x: Int, y: Int) => {
            val (xa, ya) = if ((y * cols + x) % 2 == 0)
              (0.99, -1)
            else
              (1.01, 1)

            math.atan2(xa, ya)
          },
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should arc tan all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localAtan

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.atan(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should cos all values in raster") {
        val inc = increasing

        val res = inc.localCos

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.cos(y * cols + x),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should hyperbolic cos all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localCosh

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.cosh(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should sin all values in raster") {
        val inc = increasing

        val res = inc.localSin

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.sin(y * cols + x),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should hyperbolic sin all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localSinh

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.sinh(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should tan all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localTan

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.tan(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }

      it("should hyperbolic tan all values in raster") {
        val evo = everyOther0Point99Else1Point01

        val res = evo.localTanh

        rasterShouldBe(
          res,
          (x: Int, y: Int) => math.tanh(if ((y * cols + x) % 2 == 0) 0.99 else 1.01),
          1e-4
        )

        rastersShouldHaveSameIdsAndTileCount(evo, res)
      }
    }
  }
}
