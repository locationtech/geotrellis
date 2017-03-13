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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._

/**
 * Computes the maximum value of a neighborhood for a given raster.
 *
 * @note               Maximum does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                     the data values will be rounded to integers.
 */
object Max {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = Double.NaN
          cursor.allCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if (isData(v) && (v > m || isNoData(m))) {
              m = v
            }
          }
          resultTile.setDouble(cursor.col, cursor.row, m)
        }
      }

    } else {
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = Int.MinValue
          cursor.allCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if(v > m) { m = v }
          }
          resultTile.set(cursor.col, cursor.row, m)
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}
