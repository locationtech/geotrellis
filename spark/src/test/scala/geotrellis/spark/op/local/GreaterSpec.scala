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

class GreaterSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Greater Operation") {
    ifCanRunSpark {
      val increasing = IncreasingTestFile
      val allOnes = AllOnesTestFile

      it("should check greater between an integer and a raster") {
        val inc = increasing
        val res = inc > 1

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x <= 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check greater right associative between an integer and a raster") {
        val inc = increasing
        val res = 1 >>: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 0 && y == 0) 1 else 0
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check greater between a double and a raster") {
        val inc = increasing
        val res = inc > 1.0

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x <= 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check greater right associative between a double and a raster") {
        val inc = increasing
        val res = 1.0 >>: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 0 && y == 0) 1 else 0
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check greater or equal between two rasters") {
        val inc = increasing
        val ones = allOnes
        val res = inc > ones

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x <= 1 && y == 0) 0 else 1
        )
        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }
    }
  }
}
