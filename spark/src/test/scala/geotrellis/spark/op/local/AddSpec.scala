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
import geotrellis.proj4._
import geotrellis.spark.tiling._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.RasterRDD
import geotrellis.spark.testfiles._

import org.scalatest.FunSpec

class AddSpec extends FunSpec
    with TestEnvironment
    with TestFiles
    with RasterRDDMatchers
    with OnlyIfCanRunSpark {
  describe("Add Operation") {
    ifCanRunSpark {
      val ones = AllOnesTestFile

      it("should add a constant to a raster") {
        val twos = ones + 1

        rasterShouldBe(twos, (2, 2))
        rastersShouldHaveSameIdsAndTileCount(ones, twos)
      }

      it("should add a raster to a constant") {
        val twos = 1 +: ones

        rasterShouldBe(twos, (2, 2))
        rastersShouldHaveSameIdsAndTileCount(ones, twos)
      }

      it("should add multiple rasters") {
        val threes = ones + ones + ones

        rasterShouldBe(threes, (3, 3))
        rastersShouldHaveSameIdsAndTileCount(ones, threes)
      }

      it("should add multiple rasters as a seq") {
        val threes = ones + Array(ones, ones)

        rasterShouldBe(threes, (3, 3))
        rastersShouldHaveSameIdsAndTileCount(ones, threes)
      }
    }
  }
}
