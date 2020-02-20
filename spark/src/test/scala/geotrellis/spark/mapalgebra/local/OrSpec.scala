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

class OrSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Or Operation") {
    val ones = AllOnesTestFile
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile

    it("should or a raster with a constant") {
      val res = ones | 1

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should or a constant with a raster") {
      val res = 1 |: ones

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should or three different rasters") {
      val res = ones | twos | hundreds

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should or three different rasters as a seq") {
      val res = ones | Seq(twos, hundreds)

      rasterShouldBe(res, (103, 103))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }
  }
}
