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

package geotrellis.raster.mosaic

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.raster.testkit._

import org.scalatest._

import spire.syntax.cfor._

class MosaicSpec extends FunSpec
                         with TileBuilders
                         with RasterMatchers
                         with TestFiles {

  describe("Merge functions") {
    it("should merge values from overlapping extents") {
      val tiles = Array(
        Extent(0,4,4,8) -> IntArrayTile.fill(0,4,4),
        Extent(4,4,8,8) -> IntArrayTile.fill(1,4,4),
        Extent(0,0,4,4) -> IntArrayTile.fill(2,4,4),
        Extent(4,0,8,4) -> IntArrayTile.fill(3,4,4)
      )

      val extent = Extent(2,2,6,6)
      val mergeTile = ArrayTile.empty(IntConstantNoDataCellType, 4,4)

      for ( (ex, tile) <- tiles) {
        mergeTile.merge(extent, ex, tile)
      }
      val expected = ArrayTile(Array(
        0,0,1,1,
        0,0,1,1,
        2,2,3,3,
        2,2,3,3), 4, 4)

      mergeTile should equal (expected)
    }
  }
}
