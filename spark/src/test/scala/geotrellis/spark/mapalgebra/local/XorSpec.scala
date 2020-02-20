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

package geotrellis.spark.mapalgebra.local

import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.TileLayerRDD
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class XorSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Xor Operation") {
    val ones = AllOnesTestFile
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile

    val onesST = AllOnesSpaceTime
    val twosST = AllTwosSpaceTime
    val hundredsST = AllHundredsSpaceTime

    it("should xor a raster with a constant") {
      val res = ones ^ 1

      rasterShouldBe(res, (0, 0))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should xor a spacetime raster with a constant") {
      val res = onesST ^ 1

      rasterShouldBe(res, (0, 0))
      rastersShouldHaveSameIdsAndTileCount(onesST, res)
    }

    it("should xor a constant with a raster") {
      val res = 2 ^: ones

      rasterShouldBe(res, (3, 3))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should xor a constant with a spacetime raster") {
      val res = 2 ^: onesST

      rasterShouldBe(res, (3, 3))
      rastersShouldHaveSameIdsAndTileCount(onesST, res)
    }

    it("should xor three different rasters") {
      val res = ones ^ twos ^ hundreds

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should xor three different spacetime rasters") {
      val res = onesST ^ twosST ^ hundredsST

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(onesST, res)
    }

    it("should xor three different rasters as a seq") {
      val res = ones ^ Seq(twos, hundreds)

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should xor three different spacetime rasters as a seq") {
      val res = onesST ^ Seq(twosST, hundredsST)

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(onesST, res)
    }
  }
}
