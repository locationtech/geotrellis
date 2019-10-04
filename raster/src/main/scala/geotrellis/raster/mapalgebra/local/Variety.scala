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

package geotrellis.raster.mapalgebra.local

import geotrellis.raster._

import spire.syntax.cfor._

/**
 * Variety gives the count of unique values at each location in a set of Tiles.
 *
 * @return     An IntConstantNoDataCellType raster with the count values.
 */
object Variety extends Serializable {
  def apply(rs: Traversable[Tile]): Tile =
    apply(rs.toSeq)

  def apply(rs: Tile*)(implicit d: DI): Tile =
    apply(rs)

  def apply(rs: Seq[Tile]): Tile = {
    rs.assertEqualDimensions

    val layerCount = rs.length
    if(layerCount == 0) {
      sys.error(s"Can't compute variety of empty sequence")
    } else {
      val Dimensions(cols, rows) = rs(0).dimensions
      val tile = ArrayTile.alloc(IntConstantNoDataCellType, cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val variety =
            rs.map(r => r.get(col, row))
              .toSet
              .filter(isData(_))
              .size
          tile.set(col, row, if(variety == 0) { NODATA } else { variety })
        }
      }
      tile
    }
  }
}
