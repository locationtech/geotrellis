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

package geotrellis.raster.split

import geotrellis.raster._

import spire.syntax.cfor._

object Split {
  case class Options(
    /*
     * Set the 'cropped' flag to false if you want the tiles to be
     * [[ArrayTile]]s, otherwise they will be [[CroppedTile]]s with the
     * Tile 'tile' as the backing.
     */
    cropped: Boolean = true,

    /*
     * Set the 'extend' to false if you do not want the resulting tiles
     * to extend past the input Tile's cols and rows based on the input
     * tileLayout. For instance, if the tile layout has tileRows = 50,
     * the input raster has rows = 90, and extend is false, the tiles
     * of the last row will have rows = 40 instead of rows = 50.
     */
    extend: Boolean = true
  )

  object Options {
    def DEFAULT = Options()
  }

  /**
    * Splits a [[Tile]] into an array of tiles.
    *
    *
    *
    * @param        tile           Tile to split
    * @param        tileLayout     TileLayout defining the tiles to be generated
    * @param        options        Options that control how the split happens.
    *
    * @return                      An array of Tiles
    */
  def apply(tile: Tile, tileLayout: TileLayout, options: Options): Array[Tile] = {
    val tileCols = tileLayout.tileCols
    val tileRows = tileLayout.tileRows

    val tiles = Array.ofDim[Tile](tileLayout.layoutCols * tileLayout.layoutRows)
    cfor(0)(_ < tileLayout.layoutRows, _ + 1) { layoutRow =>
      cfor(0)(_ < tileLayout.layoutCols, _ + 1) { layoutCol =>
        val firstCol = layoutCol * tileCols
        val lastCol = {
          val x = firstCol + tileCols - 1
          if(!options.extend && x > tile.cols - 1) tile.cols - 1
          else x
        }
        val firstRow = layoutRow * tileRows
        val lastRow = {
          val x = firstRow + tileRows - 1
          if(!options.extend && x > tile.rows - 1) tile.rows - 1
          else x
        }
        val gb = GridBounds(firstCol, firstRow, lastCol, lastRow)
        tiles(layoutRow * tileLayout.layoutCols + layoutCol) =
          if(options.cropped) CroppedTile(tile, gb)
          else CroppedTile(tile, gb).toArrayTile
      }
    }

    tiles
  }

  def apply(tile: Tile, tileLayout: TileLayout): Array[Tile] =
    apply(tile, tileLayout, Options.DEFAULT)
}
