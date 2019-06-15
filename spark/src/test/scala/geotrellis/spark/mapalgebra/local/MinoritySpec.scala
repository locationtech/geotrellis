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

import geotrellis.raster._

import geotrellis.spark._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class MinoritySpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Minority Operation") {
    val ones = AllOnesTestFile
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile

    it("should assign the minority of each raster, as a traversable") {
      val res = ones.localMinority(List(twos, twos, twos, hundreds, hundreds))

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the minority of each raster, as a vararg") {
      val res = ones.localMinority(twos, twos, hundreds, hundreds)

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the nth minority of each raster, as a traversable") {
      val res = ones.localMinority(1, List(twos, hundreds, ones, twos, twos))

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the nth minority of each raster, as a vararg") {
      val res = ones.localMinority(1, twos, hundreds, ones, twos, twos)

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }
  }
}
