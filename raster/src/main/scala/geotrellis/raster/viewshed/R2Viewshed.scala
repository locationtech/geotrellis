/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector._


object R2Viewshed extends Serializable {

  def apply(tile: Tile, col: Int, row: Int): Tile =
    R2Viewshed.apply(tile, col, row, 0, true)

  def apply(
    tile: Tile,
    startCol: Int, startRow: Int, height: Double,
    mode: Boolean // false => PLUS, true => OR
  ): Tile = {
    val cols = tile.cols
    val rows = tile.rows
    val re = RasterExtent(Extent(0, 0, cols, rows), cols, rows)
    val viewshed = ArrayTile.empty(IntCellType, cols, rows)
    val viewHeight = tile.getDouble(startCol, startRow) + height
    var angle = 0.0

    def callback(col: Int, row: Int) = {
      if (col == startCol && row == startRow) { // starting point
        angle = -180.0
        if (mode == false) {
          val n = {
            val n = viewshed.getDouble(col, row)
            if (isNoData(n)) 0; else n
          }
          viewshed.setDouble(col, row, n+1)
        }
        else viewshed.setDouble(col, row, 1)
      }
      else { // any other point
        val distance = math.sqrt((startCol-col)*(startCol-col) + (startRow-row)*(startRow-row))
        val alpha = math.atan((tile.getDouble(col, row) - viewHeight) / distance)

        if (angle <= alpha) {
          angle = alpha
          if (mode == false) {
            val n = {
              val n = viewshed.getDouble(col, row)
              if (isNoData(n)) 0; else n
            }
            viewshed.setDouble(col, row, n+1)
          }
          else viewshed.setDouble(col, row, 1)
        }
      }
      // println(s"XXX $col $row $angle")
    }

    var col = 0; while (col < cols) {
      Rasterizer.foreachCellInGridLine(startCol, startRow, col, 0, null, re, false)(callback) // north
      Rasterizer.foreachCellInGridLine(startCol, startRow, col, rows-1, null, re, false)(callback) // south
      col += 1
    }

    var row = 0; while (row < cols) {
      Rasterizer.foreachCellInGridLine(startCol, startRow, cols-1, row, null, re, false)(callback) // east
      Rasterizer.foreachCellInGridLine(startCol, startRow, 0, row, null, re, false)(callback) // west
      row += 1
    }

    viewshed
  }

}
