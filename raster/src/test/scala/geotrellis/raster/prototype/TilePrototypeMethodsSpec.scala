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

package geotrellis.raster.prototype

import geotrellis.raster._
import geotrellis.raster.testkit._

import org.scalatest._

class TilePrototypeMethodsSpec extends FunSpec
    with Matchers
    with TileBuilders
    with RasterMatchers {
  describe("SinglebandTileMergeMethods") {
    it("should prototype correctly for NoNoData cell types") {
      val cellTypes: Seq[CellType] =
        Seq(
          ByteCellType,
          UByteCellType,
          ShortCellType,
          UShortCellType,
          IntCellType,
          FloatCellType,
          DoubleCellType
        )

      for(ct <- cellTypes) {
        val arr = Array(0.0, 1.0, 1.0, Double.NaN)
        val tile = DoubleArrayTile(arr, 2, 2, DoubleCellType).convert(ct)
        withClue(s"Failed for cell type $ct") {
          assertEqual(tile.prototype(ct, 2, 2), ArrayTile.alloc(ct, 2, 2))
        }
      }
    }
  }
}
