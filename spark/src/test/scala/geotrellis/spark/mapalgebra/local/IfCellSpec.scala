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

class IfCellSpec extends FunSpec with TestEnvironment with TestFiles {

  describe("IfCell Operation") {
    val inc = IncreasingTestFile
    val dec = DecreasingTestFile

    val (cols: Int, rows: Int, tots: Int) = {
      val tile = inc.stitch
      (tile.cols, tile.rows, tile.cols * tile.rows - 1)
    }

    it("should change values mod 2 to 1, testing integer method") {
      val res = inc.localIf((a: Int) => a % 2 == 0, 1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else y * cols + x 
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should change values mod 2 to 1 else 0, testing integer method") {
      val res = inc.localIf((a: Int) => a % 2 == 0, 1, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 0
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should change values mod 2 to 1, testing double method") {
      val res = inc.localIf((a: Double) => a % 2 == 0, 1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else y * cols + x
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should change values mod 2 to 1 else 0, testing double method") {
      val res = inc.localIf((a: Double) => a % 2 == 0, 1, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if ((y * cols + x) % 2 == 0) 1 else 0
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 integer method") {
      val thres = tots / 2.0

      val res = inc.localIf(dec, (a: Int, b: Int) => math.abs(a - b) <= 1, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else y * cols + x
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 else 1 integer method") {
      val thres = tots / 2.0

      val res = inc.localIf(dec, (a: Int, b: Int) => math.abs(a - b) <= 1, 0, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else 0
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 double method") {
      val thres = tots / 2.0

      val res = inc.localIf(dec, (a: Double, b: Double) => math.abs(a - b) <= 1, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else y * cols + x
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }

    it("should if 2 values in 2 rasters are math.abs(diff) <= 1 set to 0 else 1 double method") {
      val thres = tots / 2.0

      val res = inc.localIf(dec, (a: Double, b: Double) => math.abs(a - b) <= 1, 0, 0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (math.abs((y * cols + x) - thres) <= 1) 0 else 0
      )

      rastersShouldHaveSameIdsAndTileCount(res, inc)
    }
  }
}
