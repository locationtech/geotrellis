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
import geotrellis.raster.Tile
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class MinSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Min Operation") {
    val inc = IncreasingTestFile
    val dec = DecreasingTestFile
    val hundreds = AllHundredsTestFile

    it("should min a raster with an integer") {
      val thresh = 721.1
      val res = inc.localMin(thresh)
      rasterShouldBeAbout(
        res,
        (tile: Tile, x: Int, y: Int) => math.min(y * tile.cols + x, thresh),
        1e-3
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should min a raster with a double") {
      val thresh = 873.4
      val res = inc.localMin(thresh)

      rasterShouldBeAbout(
        res,
        (t: Tile, x: Int, y: Int) => math.min(y * t.cols + x, thresh),
        1e-3
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should min two rasters") {
      val res = inc.localMin(dec)

      rasterShouldBe(
        res,
        (t: Tile, x: Int, y: Int) => {
          val decV = t.cols * t.rows - (y * t.cols + x) - 1
          val incV = y * t.cols + x

          math.min(decV, incV)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should min three rasters as a seq") {
      val res = inc.localMin(Seq(dec, hundreds))

      rasterShouldBe(
        res,
        (t: Tile, x: Int, y: Int) => {
          val decV = t.cols * t.rows - (y * t.cols + x) - 1
          val incV = y * t.cols + x

          math.min(math.min(decV, incV), 100)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }
  }
}
