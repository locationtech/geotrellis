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

class PowSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Pow Operation") {
    val hundreds = AllHundredsTestFile
    val twos = AllTwosTestFile

    it("should pow a raster with an integer") {
      val res = twos ** 2

      rasterShouldBe(res, (4, 4))

      rastersShouldHaveSameIdsAndTileCount(res, twos)
    }

    it("should pow a integer with a raster") {
      val res = 3 **: twos

      rasterShouldBe(res, (9, 9))

      rastersShouldHaveSameIdsAndTileCount(res, twos)
    }

    it("should pow a raster with an double") {
      val res = twos ** 1.5

      rasterShouldBeAbout(res, (x: Int, y: Int) => math.pow(2, 1.5), 1e-6)

      rastersShouldHaveSameIdsAndTileCount(res, twos)
    }

    it("should pow a double with a raster") {
      val res = 1.5 **: twos

      rasterShouldBeAbout(res, (x: Int, y: Int) => math.pow(1.5, 2), 1e-10)

      rastersShouldHaveSameIdsAndTileCount(res, twos)
    }


    it("should pow two rasters") {
      val res = hundreds ** twos

      rasterShouldBe(res, (1e4.toInt, 1e4.toInt))

      rastersShouldHaveSameIdsAndTileCount(res, hundreds)
    }

    it("should pow three rasters as a seq") {
      val res = hundreds ** Seq(twos, twos)

      rasterShouldBe(res, (1e8.toInt, 1e8.toInt))

      rastersShouldHaveSameIdsAndTileCount(res, hundreds)
    }
  }
}
