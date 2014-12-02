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

class UnEqualSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("UnEqual Operation") {
    ifCanRunSpark {
      val inc = IncreasingTestFile
      val ones = AllOnesTestFile

      it("should check unEqual between an integer and a raster") {
        val res = inc !== 1

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check unEqual between a double and a raster") {
        val res = inc !== 1.0

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check unEqual between a raster and an integer") {
        val res = 1 !==: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check unEqual between a raster and a double") {
        val res = 1.0 !==: inc

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }

      it("should check unEqual between two rasters") {
        val res = inc !== ones

        rasterShouldBe(
          res,
          (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
        )

        rastersShouldHaveSameIdsAndTileCount(inc, res)
      }
    }
  }
}
