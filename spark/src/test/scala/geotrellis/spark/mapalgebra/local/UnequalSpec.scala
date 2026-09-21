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

import geotrellis.spark.*
import geotrellis.spark.testkit.testfiles.*
import geotrellis.spark.testkit.*

import org.scalatest.funspec.AnyFunSpec
import geotrellis.util.identityComponent

class UnequalSpec extends AnyFunSpec with TestEnvironment with TestFiles {

  describe("UnEqual Operation") {
    val inc = IncreasingTestFile
    val ones = AllOnesTestFile

    val onesST = AllOnesSpaceTime
    val twosST = AllTwosSpaceTime

    it("should check unEqual between an integer and a raster") {
      val res = inc.localUnequal(1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should check unEqual between an integer and a spacetime raster") {
      val res1 = onesST.localUnequal(1)

      rasterShouldBe(
        res1,
        0,
        210
      )

      val res2 = twosST.localUnequal(1)
      rasterShouldBe(
        res2,
        1,
        210
      )

      rastersShouldHaveSameIdsAndTileCount(onesST, res1)
      rastersShouldHaveSameIdsAndTileCount(onesST, res2)
    }

    it("should check unEqual between a double and a raster") {
      val res = inc.localUnequal(1.0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should check unEqual between an double and a spacetime raster") {
      val res1 = onesST.localUnequal(1.0)

      rasterShouldBe(
        res1,
        0,
        210
      )

      val res2 = twosST.localUnequal(1)
      rasterShouldBe(
        res2,
        1,
        210
      )

      rastersShouldHaveSameIdsAndTileCount(onesST, res1)
      rastersShouldHaveSameIdsAndTileCount(onesST, res2)
    }

    it("should check unEqual between a raster and an integer") {
      val res = inc.localUnequal(1)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should check unEqual between an spacetime raster and an integer") {
      val res1 = onesST.localUnequal(1)

      rasterShouldBe(
        res1,
        0,
        210
      )

      val res2 = twosST.localUnequal(1)
      rasterShouldBe(
        res2,
        1,
        210
      )

      rastersShouldHaveSameIdsAndTileCount(onesST, res1)
      rastersShouldHaveSameIdsAndTileCount(onesST, res2)
    }

    it("should check unEqual between a raster and a double") {
      val res = inc.localUnequal(1.0)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should check unEqual between a spacetime raster and a double") {
      val res1 = onesST.localUnequal(1.0)

      rasterShouldBe(
        res1,
        0,
        210
      )

      val res2 = twosST.localUnequal(1)
      rasterShouldBe(
        res2,
        1,
        210
      )

      rastersShouldHaveSameIdsAndTileCount(onesST, res1)
      rastersShouldHaveSameIdsAndTileCount(onesST, res2)
    }

    it("should check unEqual between two rasters") {
      val res = inc.localUnequal(ones)

      rasterShouldBe(
        res,
        (x: Int, y: Int) => if (x == 1 && y == 0) 0 else 1
      )

      rastersShouldHaveSameIdsAndTileCount(inc, res)
    }

    it("should check unEqual between two spacetime rasters") {
      val res1 = onesST.localUnequal(onesST)

      rasterShouldBe(
        res1,
        0,
        210
      )

      val res2 = onesST.localUnequal(twosST)
      rasterShouldBe(
        res2,
        1,
        210
      )

      rastersShouldHaveSameIdsAndTileCount(onesST, res1)
      rastersShouldHaveSameIdsAndTileCount(onesST, res2)
    }
  }
}
