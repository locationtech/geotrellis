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
 * Computes the minimum value of a neighborhood for a given raster
 */
object Min {
  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {

    if (tile.cellType.isFloatingPoint)
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m: Double = Double.NaN
          cursor.allCells.foreach { (col, row) =>
            val v = r.getDouble(col, row)
            if (isData(v) && (v < m || isNoData(m))) {
              m = v
            }
          }
          resultTile.setDouble(cursor.col, cursor.row, m)
        }
      }

    else
      new CursorCalculation[Tile](tile, n, bounds, target)
        with ArrayTileResult
      {
        def calc(r: Tile, cursor: Cursor) = {
          var m = NODATA
          cursor.allCells.foreach { (col, row) =>
            val v = r.get(col, row)
            if(isData(v) && (v < m || isNoData(m))) { m = v }
          }

          resultTile.set(cursor.col, cursor.row, m)
        }
      }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}
