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

package geotrellis.raster.mapalgebra.zonal

import geotrellis.raster._

import geotrellis.raster.testkit._
import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class ZonalPercentageSpec extends AnyFunSpec
                             with Matchers
                             with RasterMatchers
                             with TileBuilders {
  describe("ZonalPercentage") {
    it("gives correct percentage for example raster") {
      val r = createTile(
        Array(1, 2, 2, 2, 3, 1, 6, 5, 1,
              1, 2, 2, 2, 3, 6, 6, 5, 5,
              1, 3, 5, 3, 6, 6, 6, 5, 5,
              3, 1, 5, 6, 6, 6, 6, 6, 2,
              7, 7, 5, 6, 1, 3, 3, 3, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 7, 5, 5, 5, 4, 3, 4, 2,
              7, 2, 2, 5, 4, 4, 3, 4, 4),
        9,8)

      // 1 - 9
      // 2 - 15
      // 3 - 12
      // 4 - 6
      // 5 - 6
      // 6 - 6
      // 7 - 12
      // 8 - 6
      val zones = createTile(
        Array(1, 1, 1, 4, 4, 4, 5, 6, 6,
              1, 1, 1, 4, 4, 5, 5, 6, 6,
              1, 1, 2, 4, 5, 5, 5, 6, 6,
              1, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 3, 3, 3, 3, 3, 3,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8,
              2, 2, 2, 7, 7, 7, 7, 8, 8),
        9,8)

      val expected = Map(
        1 -> Map(
          1 -> 33,
          2 -> 44,
          3 -> 22
        ),
        2 -> Map(
          7 -> 47,
          2 -> 13,
          5 -> 33,
          1 ->  7
        ),
        3 -> Map(
          1 -> 8,
          6 -> 50,
          3 -> 25,
          2 -> 17
        ),
        4 -> Map(
          1 -> 17,
          2 -> 33,
          3 -> 50
        ),
        5 -> Map(
          6 -> 100
        ),
        6 -> Map(
          1 -> 17,
          5 -> 83
        ),
        7 -> Map(
          3 -> 25,
          4 -> 33,
          5 -> 42
        ),
        8 -> Map(
          2 -> 33,
          4 -> 67
        )
      )

      val result = r.zonalPercentage(zones)

      val (cols,rows) = (result.cols, result.rows)
      cfor(0)(_ < r.rows, _ + 1) { row =>
        cfor(0)(_ < r.cols, _ + 1) { col =>
          val zone = zones.get(col,row)
          val value = r.get(col,row)
          val percentage = result.get(col,row)
          percentage should be (expected(zone)(value))
        }
      }
    }
  }
}
