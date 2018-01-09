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

package geotrellis.raster.transform

import geotrellis.raster._
import spire.syntax.cfor._

trait TransformTileMethods extends TransformMethods[Tile] {
  /**
    * Rotate [[Tile]] n times by 90 degrees in the counter-clockwise direction.
    * @param n   number of times to rotate tile
    */
  def rotate90(n: Int = 1): Tile = {
    val (rows, cols) = self.rows -> self.cols
    if (n % 4 == 0) self
    else if (4 * math.ceil(n.toDouble / 4).toInt - 1 == n) {
      val tile = ArrayTile.alloc(self.cellType, self.rows, self.cols)
      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(rows - 1 - row, col, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(rows - 1 - row, col, self.getDouble(col, row))
          }
        }
      }
      tile
    } else if (n % 2 == 0) {
      val tile = ArrayTile.alloc(self.cellType, self.cols, self.rows)
      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(cols - 1 - col, rows - 1 - row, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(cols - 1 - col, rows - 1 - row, self.getDouble(col, row))
          }
        }
      }
      tile
    } else {
      val tile = ArrayTile.alloc(self.cellType, self.rows, self.cols)
      if (!self.cellType.isFloatingPoint) {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.set(row, cols - 1 - col, self.get(col, row))
          }
        }
      } else {
        cfor(0)(_ < cols, _ + 1) { col =>
          cfor(0)(_ < rows, _ + 1) { row =>
            tile.setDouble(row, cols - 1 - col, self.getDouble(col, row))
          }
        }
      }
      tile
    }
  }

  def flipVertical: Tile = {
    val (rows, cols) = self.rows -> self.cols
    val tile = ArrayTile.alloc(self.cellType, cols, rows)

    if (!self.cellType.isFloatingPoint) {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.set(cols - 1 - col, row, self.get(col, row))
          tile.set(col, row, self.get(cols - col - 1, row))
        }
      }
    } else {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.setDouble(cols - 1 - col, row, self.getDouble(col, row))
          tile.setDouble(col, row, self.getDouble(cols - col - 1, row))
        }
      }
    }

    tile
  }

  def flipHorizontal: Tile = {
    val (rows, cols) = self.rows -> self.cols
    val tile = ArrayTile.alloc(self.cellType, cols, rows)

    if (!self.cellType.isFloatingPoint) {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.set(col, rows - 1 - row, self.get(col, row))
          tile.set(col, row, self.get(col, rows - 1 - row))
        }
      }
    } else {
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          tile.setDouble(col, rows - 1 - row, self.getDouble(col, row))
          tile.setDouble(col, row, self.getDouble(col, rows - 1 - row))
        }
      }
    }

    tile
  }
}
