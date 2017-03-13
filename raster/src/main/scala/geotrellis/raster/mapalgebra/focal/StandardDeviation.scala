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
 * Computes the standard deviation of a neighborhood for a given raster. Returns a raster of DoubleConstantNoDataCellType.
 *
 * @note            StandardDeviation does not currently support Double raster data inputs.
 *                  If you use a Tile with a Double CellType (FloatConstantNoDataCellType, DoubleConstantNoDataCellType)
 *                  the data values will be rounded to integers.
 */
object StandardDeviation {

  def calculation(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): FocalCalculation[Tile] = {
    if(tile.cellType.isFloatingPoint) {
      new CursorCalculation[Tile](tile, n, bounds, target)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Double = 0

        def calc(r: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) {
              count -= 1
              if (count == 0) sum = 0 else sum -= v
            }
          }

          c.addedCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) { count += 1; sum += v }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = r.getDouble(x, y)
            if(isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }

          resultTile.setDouble(c.col, c.row, math.sqrt(squares / count.toDouble))
        }
      }
    } else {
      new CursorCalculation[Tile](tile, n, bounds, target)
        with DoubleArrayTileResult
      {
        var count: Int = 0
        var sum: Int = 0

        def calc(r: Tile, c: Cursor) = {
          c.removedCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              count -= 1; sum -= v
            }
          }

          c.addedCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              count += 1; sum += v
            }
          }

          val mean = sum / count.toDouble
          var squares = 0.0

          c.allCells.foreach { (x, y) =>
            val v = r.get(x, y)
            if (isData(v)) {
              squares += math.pow(v - mean, 2)
            }
          }
          resultTile.setDouble(c.col, c.row, math.sqrt(squares / count.toDouble))
        }
      }
    }
  }

  def apply(tile: Tile, n: Neighborhood, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, n, bounds, target).execute()
}
