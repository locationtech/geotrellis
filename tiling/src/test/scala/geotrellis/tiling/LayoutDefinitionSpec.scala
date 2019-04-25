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

package geotrellis.tiling

import geotrellis.raster._
import geotrellis.vector.Extent

import org.scalatest._

class LayoutDefinitionSpec extends FunSpec with Matchers {
  describe("LayoutDefinition"){
    it("should not buffer the extent of a grid that fits within it's bounds"){
      val e = Extent(-31.4569758,  27.6350020, 40.2053192,  80.7984255)
      val cs = CellSize(0.08332825, 0.08332825)
      val tileSize = 256
      val ld = LayoutDefinition(GridExtent[Long](e, cs), tileSize, tileSize)
      val ld2 = LayoutDefinition(GridExtent[Long](ld.extent, cs), ld.tileCols, ld.tileRows)

      ld2.extent should be (ld.extent)
    }
    val grid = GridExtent[Long](Extent(-180, -90, 180, 90), CellSize(0.001, 0.001))
    val layout = LayoutDefinition(grid, 300, 300)

    it("not equal its GridExtent") {
      layout.cellSize shouldBe grid.cellSize
      (layout.cols) shouldBe grid.cols
      (layout.rows) shouldBe grid.rows
      layout should not equal (grid)
    }
  }
}
