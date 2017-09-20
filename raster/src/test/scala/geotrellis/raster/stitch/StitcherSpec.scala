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

package geotrellis.raster.stitch

import geotrellis.raster._
import geotrellis.raster.testkit._

import org.scalatest._

class StitcherSpec extends FunSpec with Matchers 
                                   with TileBuilders {
  describe("Stitcher[Tile]") {
    it("should stitch a buffered tile with top missing") {
      val tiles = 
        Seq(
          (IntArrayTile(Array(1, 1), 1, 2), (0, 0)), // Left
          (IntArrayTile(Array(3, 3, 3), 3, 1), (1, 2)), // Bottom
          (IntArrayTile(Array(1, 3, 5, 2, 2, 2), 3, 2), (1, 0)), // Center
          (IntArrayTile(Array(1), 1, 1), (0, 2)), //Bottom left
          (IntArrayTile(Array(1), 1, 1), (4, 2)), // Bottom Right
          (IntArrayTile(Array(9, 4), 1, 2), (4, 0)) // Right
        )
      val actual = implicitly[Stitcher[Tile]].stitch(tiles, 5, 3)

      val expected = 
        ArrayTile(Array(
          1,   1, 3, 5,   9,
          1,   2, 2, 2,   4,
          1,   3, 3, 3,   1
        ), 5, 3)

      actual.toArray should be (expected.toArray)
    }
  }
}
