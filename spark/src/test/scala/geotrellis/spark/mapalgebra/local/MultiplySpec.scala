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

class MultiplySpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Multiply Operation") {
    val twos = AllTwosTestFile

    it("should multiply a constant by a raster") {
      val fours = twos * 2

      rasterShouldBe(fours, (4, 4))
      rastersShouldHaveSameIdsAndTileCount(twos, fours)
    }

    it("should multiply a raster by a constant") {
      val fours = 2 *: twos

      rasterShouldBe(fours, (4, 4))
      rastersShouldHaveSameIdsAndTileCount(twos, fours)
    }

    it("should multiply multiple rasters") {
      val eights = twos * twos * twos

      rasterShouldBe(eights, (8, 8))
      rastersShouldHaveSameIdsAndTileCount(twos, eights)
    }

    it("should multiply multiple rasters as a seq") {
      val eights = twos * Seq(twos, twos)

      rasterShouldBe(eights, (8, 8))
      rastersShouldHaveSameIdsAndTileCount(twos, eights)
    }
  }
}
