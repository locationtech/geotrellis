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
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class SubtractSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Subtract Operation") {
    val ones = AllOnesTestFile
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile

    it("should subtract a constant from a raster") {
      val res = twos - 1

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(twos, res)
    }

    it("should subtract from a constant, raster values") {
      val res = 3 -: twos

      rasterShouldBe(ones, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(twos, res)
    }

    it("should subtract multiple rasters") {
      val res = hundreds - twos - ones

      rasterShouldBe(res, (97, 97))
      rastersShouldHaveSameIdsAndTileCount(hundreds, res)
    }

    it("should subtract multiple rasters as a seq") {
      val res = hundreds - Seq(twos, ones)

      rasterShouldBe(res, (97, 97))
      rastersShouldHaveSameIdsAndTileCount(hundreds, res)
    }
  }
}
