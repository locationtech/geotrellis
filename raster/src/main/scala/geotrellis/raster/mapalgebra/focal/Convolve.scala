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
import spire.syntax.cfor._

/**
 * Computes the convolution of a raster with a kernel.
 *
 * @param      tile         Tile to convolve.
 * @param      kernel       Kernel that represents the convolution filter.
 * @param      bounds       Optionla bounds of the analysis area that we are convolving.
 */
object Convolve {
  def calculation(tile: Tile, kernel: Kernel, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): KernelCalculation[Tile] = {
    if (tile.cellType.isFloatingPoint) {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel, bounds, target) with ArrayTileResult {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = Double.NaN
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = t.getDouble(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            resultTile.setDouble(cursor.col, cursor.row, s)
          }
        }
      } else {
        new KernelCalculation[Tile](tile, kernel, bounds, target) with ArrayTileResult {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = Double.NaN
            cursor.foreachWithWeight { (x, y, w) =>
              val v = t.getDouble(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            resultTile.setDouble(cursor.col, cursor.row, s)
          }
        }
      }
    } else {
      if(kernel.cellType.isFloatingPoint) {
        new KernelCalculation[Tile](tile, kernel, bounds, target) with ArrayTileResult {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeightDouble { (x, y, w) =>
              val v = t.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w).toInt }
                else { s = (v * w).toInt }
              }
            }
            resultTile.set(cursor.col, cursor.row, s)
          }
        }
      } else {
        new KernelCalculation[Tile](tile, kernel, bounds, target) with ArrayTileResult {
          def calc(t: Tile, cursor: KernelCursor) = {
            var s = NODATA
            cursor.foreachWithWeight { (x, y, w) =>
              val v = t.get(x, y)
              if(isData(v)) {
                if(isData(s)) { s = s + (v * w) }
                else { s = v * w }
              }
            }
            resultTile.set(cursor.col, cursor.row, s)
          }
        }
      }
    }
  }

  def apply(tile: Tile, kernel: Kernel, bounds: Option[GridBounds] = None, target: TargetCell = TargetCell.All): Tile =
    calculation(tile, kernel, bounds, target).execute()
}
