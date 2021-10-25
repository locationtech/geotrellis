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

package geotrellis.raster

import geotrellis.raster.testkit._

import spire.syntax.cfor._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class DoubleArrayTileSpec extends AnyFunSpec with Matchers with RasterMatchers with TileBuilders {

  describe("DoubleArrayTile.toByteArray") {
    it("converts back and forth.") {
      val tile = probabilityRaster
      val (cols, rows) = (tile.cols, tile.rows)
      val tile2 = DoubleArrayTile.fromBytes(tile.toBytes(), cols, rows)
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          withClue(s"Values different at ($col, $row)") {
            tile2.getDouble(col, row) should be (tile.getDouble(col, row))
          }
        }
      }
    }
  }
}
