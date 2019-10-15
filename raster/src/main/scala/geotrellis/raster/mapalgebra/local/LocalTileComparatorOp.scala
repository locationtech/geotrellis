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

trait LocalTileComparatorOp extends Serializable {
  val name = {
    val n = getClass.getSimpleName
    if (n.endsWith("$")) n.substring(0, n.length - 1)
    else n
  }
  // Tile - Constant combinations

  /** Apply to the value from each cell and a constant Int. */
  def apply(r: Tile, c: Int): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)
    if (r.cellType.isFloatingPoint) {
      val cons = c.toDouble
      cfor(0)(_ < r.rows, _ + 1) { row =>
        cfor(0)(_ < r.cols, _ + 1) { col =>
          tile.set(col, row, if (compare(r.getDouble(col, row), cons)) 1 else 0)
        }
      }
    } else {
      cfor(0)(_ < r.rows, _ + 1) { row =>
        cfor(0)(_ < r.cols, _ + 1) { col =>
          tile.set(col, row, if (compare(r.get(col, row), c)) 1 else 0)
        }
      }
    }
    tile
  }

  /** Apply to the value from each cell and a constant Double. */
  def apply(r: Tile, c: Double): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    cfor(0)(_ < r.rows, _ + 1) { row =>
      cfor(0)(_ < r.cols, _ + 1) { col =>
        tile.set(col, row, if (compare(r.getDouble(col, row), c)) 1 else 0)
      }
    }
    tile
  }

  /** Apply to a constant Int and the value from each cell. */
  def apply(c: Int, r: Tile): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    if (r.cellType.isFloatingPoint) {
      val cons = c.toDouble
      cfor(0)(_ < r.rows, _ + 1) { row =>
        cfor(0)(_ < r.cols, _ + 1) { col =>
          tile.set(col, row, if (compare(cons, r.getDouble(col, row))) 1 else 0)
        }
      }
    } else {
      cfor(0)(_ < r.rows, _ + 1) { row =>
        cfor(0)(_ < r.cols, _ + 1) { col =>
          tile.set(col, row, if (compare(c, r.get(col, row))) 1 else 0)
        }
      }
    }
    tile
  }

  /** Apply to a constant Double and the value from each cell. */
  def apply(c: Double, r: Tile): Tile = {
    val tile = BitArrayTile.ofDim(r.cols, r.rows)

    cfor(0)(_ < r.rows, _ + 1) { row =>
      cfor(0)(_ < r.cols, _ + 1) { col =>
        tile.set(col, row, if (compare(c, r.getDouble(col, row))) 1 else 0)
      }
    }
    tile
  }

  // Tile - Tile combinations

  /** Apply this operation to the values of each cell in each raster.  */
  def apply(r1: Tile, r2: Tile): Tile = {
    Traversable(r1, r2).assertEqualDimensions
    val Dimensions(cols, rows) = r1.dimensions
    val tile = BitArrayTile.ofDim(cols, rows)

    cfor(0)(_ < r1.rows, _ + 1) { row =>
      cfor(0)(_ < r1.cols, _ + 1) { col =>
        if (r1.cellType.isFloatingPoint || r2.cellType.isFloatingPoint) {
          tile.set(col, row, if (compare(r1.getDouble(col, row), r2.getDouble(col, row))) 1 else 0)
        } else {
          tile.set(col, row, if (compare(r1.get(col, row), r2.get(col, row))) 1 else 0)
        }
      }
    }
    tile
  }

  def compare(z1: Int, z2: Int): Boolean
  def compare(z1: Double, z2: Double): Boolean
}
