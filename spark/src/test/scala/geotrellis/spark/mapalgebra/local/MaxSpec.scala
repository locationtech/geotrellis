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
import geotrellis.raster._
import geotrellis.spark.store.hadoop._
import geotrellis.spark.TileLayerRDD
import geotrellis.spark.testkit.testfiles._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class MaxSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Max Operation") {
    val inc = IncreasingTestFile
    val dec = DecreasingTestFile
    val hundreds = AllHundredsTestFile

    val tots = 2342523;

    it("should max a raster with an integer") {
      val thresh = tots / 2
      val res = inc.localMax(thresh)

      rasterShouldBe(
        res,
        (tile: Tile, x: Int, y: Int) => math.max(y * tile.cols + x, thresh)
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should max a raster with a double") {
      val thresh = tots / 2.0
      val res = inc.localMax(thresh)

      rasterShouldBe(
        res,
        (tile: Tile, x: Int, y: Int) => math.max(y * tile.cols + x, thresh)
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should max two rasters") {
      val res = inc.localMax(dec)

      rasterShouldBe(
        res,
        (tile: Tile, x: Int, y: Int) => {
          val decV = tile.cols * tile.rows - (y * tile.cols + x) - 1
          val incV = y * tile.cols + x

          math.max(decV, incV)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should max three rasters as a seq") {
      val res = inc.localMax(Seq(dec, hundreds))

      rasterShouldBe(
        res,
        (tile: Tile, x: Int, y: Int) => {
          val decV = tile.cols * tile.rows - (y * tile.cols + x) - 1
          val incV = y * tile.cols + x

          math.max(math.max(decV, incV), 100)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }
  }
}
