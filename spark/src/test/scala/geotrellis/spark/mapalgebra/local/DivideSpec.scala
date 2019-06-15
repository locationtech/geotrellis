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

class DivideSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Divide Operation") {
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile

    it("should divide raster values by a constant") {
      val ones = twos / 2

      rasterShouldBe(ones, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(twos, ones)
    }

    it("should divide from a constant, raster values") {
      val ones = 2 /: twos

      rasterShouldBe(ones, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(twos, ones)
    }

    it("should divide multiple rasters") {
      val res = hundreds / twos / twos

      rasterShouldBe(res, (25, 25))
      rastersShouldHaveSameIdsAndTileCount(hundreds, res)
    }

    it("should divide multiple rasters as a seq") {
      val res = hundreds / Seq(twos, twos)

      rasterShouldBe(res, (25, 25))
      rastersShouldHaveSameIdsAndTileCount(hundreds, res)
    }
  }
}
