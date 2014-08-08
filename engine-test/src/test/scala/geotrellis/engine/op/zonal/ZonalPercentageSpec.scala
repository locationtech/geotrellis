/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.engine.op.zonal

import geotrellis.raster._
import geotrellis.engine._

import org.scalatest._

import geotrellis.testkit._

import scala.collection.mutable

class ZonalPercentageSpec extends FunSpec 
                             with Matchers 
                             with TestEngine 
                             with TileBuilders {
  describe("ZonalPercentage") {
    it("gives correct percentage for example raster sources") { 
      val rs = createRasterSource(
        Array(1, 2, 2,   2, 3, 1,   6, 5, 1,
              1, 2, 2,   2, 3, 6,   6, 5, 5,

              1, 3, 5,   3, 6, 6,   6, 5, 5,
              3, 1, 5,   6, 6, 6,   6, 6, 2,

              7, 7, 5,   6, 1, 3,   3, 3, 2,
              7, 7, 5,   5, 5, 4,   3, 4, 2,

              7, 7, 5,   5, 5, 4,   3, 4, 2,
              7, 2, 2,   5, 4, 4,   3, 4, 4),
        3,4,3,2)

      // 1 - 9
      // 2 - 15
      // 3 - 12
      // 4 - 6
      // 5 - 6
      // 6 - 6
      // 7 - 12
      // 8 - 6
      val zonesSource = createRasterSource(
        Array(1, 1, 1,   4, 4, 4,   5, 6, 6,
              1, 1, 1,   4, 4, 5,   5, 6, 6,

              1, 1, 2,   4, 5, 5,   5, 6, 6,
              1, 2, 2,   3, 3, 3,   3, 3, 3,

              2, 2, 2,   3, 3, 3,   3, 3, 3,
              2, 2, 2,   7, 7, 7,   7, 8, 8,

              2, 2, 2,   7, 7, 7,   7, 8, 8,
              2, 2, 2,   7, 7, 7,   7, 8, 8),
        3,4,3,2)

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

      val result = get(rs.zonalPercentage(zonesSource))
      val r = get(rs)
      val zones = get(zonesSource)
      val (cols,rows) = (r.cols, r.rows)
      for(col <- 0 until cols) {
        for(row <- 0 until rows) {
          val zone = zones.get(col,row)
          val value = r.get(col,row)
          val percentage = result.get(col,row)
          withClue(s"Expected($zone)($value) = ${expected(zone)(value)}, Actual = $percentage") {
            percentage should be (expected(zone)(value))
          }
        }
      }
    }
  }
}
