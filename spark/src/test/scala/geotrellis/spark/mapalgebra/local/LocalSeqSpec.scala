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

class LocalSeqSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("Local Seq Operations") {
    val ones = AllOnesTestFile
    val twos = AllTwosTestFile
    val hundreds = AllHundredsTestFile
    val inc = IncreasingTestFile
    val dec = DecreasingTestFile

    val (cols: Int, rows: Int) = {
      val tile = ones.stitch
      (tile.cols, tile.rows)
    }

    it("should test raster rdd seq with one element") {
      val res = Seq(ones).localAdd

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should add rasters") {
      val res = Seq(ones, hundreds, ones).localAdd

      rasterShouldBe(res, (102, 102))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should get variety of rasters") {
      val res = Seq(ones, hundreds, ones).localVariety

      rasterShouldBe(res, (2, 2))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should get mean of rasters") {
      val res = Seq(ones, hundreds, ones).localMean

      rasterShouldBe(res, (34, 34))
      rastersShouldHaveSameIdsAndTileCount(ones, res)
    }

    it("should min three rasters as a seq") {
      val res = Seq(inc, dec, hundreds).localMin

      rasterShouldBe(
        res,
        (x: Int, y: Int) => {
          val decV = cols * rows - (y * cols + x) - 1
          val incV = y * cols + x

          math.min(math.min(decV, incV), 100)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should min three rasters as a seq and take n:th smallest") {
      val res = Seq(inc, dec, hundreds).localMinN(1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => {
          val decV = cols * rows - (y * cols + x) - 1
          val incV = y * cols + x

          val d = Array(decV, incV, 100).sorted
          d(1)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should max three rasters as a seq") {
      val res = Seq(inc, dec, hundreds).localMax

      rasterShouldBe(
        res,
        (x: Int, y: Int) => {
          val decV = cols * rows - (y * cols + x) - 1
          val incV = y * cols + x

          math.max(math.max(decV, incV), 100)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should max three rasters as a seq and take n:th smallest") {
      val res = Seq(inc, dec, hundreds).localMaxN(1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => {
          val decV = cols * rows - (y * cols + x) - 1
          val incV = y * cols + x

          val d = Array(decV, incV, 100).sorted
          d(1)
        }
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should assign the minority of each raster") {
      val res = Seq(ones, twos, twos, hundreds, hundreds).localMinority()

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the nth minority of each raster") {
      val res = Seq(ones, twos, twos, twos, hundreds, hundreds).localMinority(1)

      rasterShouldBe(res, (100, 100))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the majority of each raster") {
      val res = Seq(ones, ones, ones, twos, twos, hundreds).localMajority()

      rasterShouldBe(res, (1, 1))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }

    it("should assign the nth majority of each raster") {
      val res = Seq(ones, ones, ones, twos, twos, hundreds).localMajority(1)

      rasterShouldBe(res, (2, 2))
      rastersShouldHaveSameIdsAndTileCount(res, ones)
    }
  }
}
