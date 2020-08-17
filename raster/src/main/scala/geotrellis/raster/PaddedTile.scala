/*
 * Copyright 2019 Azavea
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

package geotrellis.raster

import spire.syntax.cfor._

case class PaddedTile(chunk: Tile, colOffset: Int, rowOffset: Int, cols: Int, rows: Int) extends Tile {
  private val chunkBounds = GridBounds(
    colMin = colOffset,
    rowMin = rowOffset,
    colMax = colOffset + chunk.cols - 1,
    rowMax = rowOffset + chunk.rows - 1
  )

  require(colOffset >= 0 && rowOffset >= 0 && colOffset < cols && rowOffset < rows,
    s"chunk offset out of bounds: $colOffset, $rowOffset")

  require((chunk.cols + colOffset <= cols) && (chunk.rows + rowOffset <= rows),
    s"chunk at $chunkBounds exceeds tile boundary at ($cols, $rows)")

  def cellType = chunk.cellType

  def convert(cellType: CellType): Tile =
    copy(chunk = chunk.convert(cellType))

  def withNoData(noDataValue: Option[Double]): Tile =
    copy(chunk = chunk.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile =
    copy(chunk = chunk.interpretAs(newCellType))

  def get(col: Int, row: Int): Int = {
    if (chunkBounds.contains(col, row))
      chunk.get(col - colOffset, row - rowOffset)
    else NODATA
  }

  def getDouble(col: Int, row: Int): Double = {
    if (chunkBounds.contains(col, row))
      chunk.getDouble(col - colOffset, row - rowOffset)
    else Double.NaN
  }

  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row)))
      }
    }
    tile
  }

  def mapDouble(f: Double => Double): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row)))
      }
    }
    tile
  }

  def foreach(f: Int => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      if (row < rowOffset || row > (rowOffset + rows - 1)) {
        cfor(0)(_ < cols, _ + 1) { _ =>
          f(NODATA)
        }
      } else {
        cfor(0)(_ < colOffset, _ + 1) { _ =>
          f(NODATA)
        }
        cfor(0)(_ < chunk.cols, _ + 1) { col =>
          f(chunk.get(col, row - rowOffset))
        }
        cfor(colOffset + chunk.cols)(_ < cols, _ + 1) { _ =>
          f(NODATA)
        }
      }
    }
  }

  def foreachDouble(f: Double => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      if (row < rowOffset || row > (rowOffset + chunk.rows) - 1) {
        cfor(0)(_ < cols, _ + 1) { _ =>
          f(Double.NaN)
        }
      } else {
        cfor(0)(_ < colOffset, _ + 1) { _ =>
          f(Double.NaN)
        }
        cfor(0)(_ < chunk.cols, _ + 1) { col =>
          f(chunk.getDouble(col, row - rowOffset))
        }
        cfor(colOffset + chunk.cols)(_ < cols, _ + 1) { _ =>
          f(Double.NaN)
        }
      }
    }
  }

  def mutable(): MutableArrayTile =
    mutable(cellType)

  def mutable(targetCellType: CellType): MutableArrayTile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    if(!cellType.isFloatingPoint) {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          tile.set(col, row, get(col, row))
        }
      }
    } else {
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          tile.setDouble(col, row, getDouble(col, row))
        }
      }
    }

    tile
  }


  def toArrayTile(): ArrayTile = mutable

  def toArray(): Array[Int] = {
    val arr = Array.ofDim[Int](cols * rows)

    var i = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        arr(i) = get(col, row)
        i += 1
      }
    }

    arr
  }

  def toArrayDouble(): Array[Double] = {
    val arr = Array.ofDim[Double](cols * rows)

    var i = 0
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        arr(i) = getDouble(col, row)
        i += 1
      }
    }

    arr
  }

  def toBytes(): Array[Byte] = toArrayTile.toBytes


  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row), other.get(col, row)))
      }
    }

    tile
  }

  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
      }
    }

    tile
  }

  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      if (row < rowOffset || row > (rowOffset + rows - 1)) {
        cfor(0)(_ < cols, _ + 1) { col =>
          visitor(col, row, NODATA)
        }
      } else {
        cfor(0)(_ < colOffset, _ + 1) { col =>
          visitor(col, row, NODATA)
        }
        cfor(0)(_ < chunk.cols, _ + 1) { col =>
          visitor(col + colOffset, row, chunk.get(col, row - rowOffset))
        }
        cfor(colOffset + chunk.cols)(_ < cols, _ + 1) { col =>
          visitor(col, row, NODATA)
        }
      }
    }
  }

  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      if (row < rowOffset || row > (rowOffset + rows - 1)) {
        cfor(0)(_ < cols, _ + 1) { col =>
          visitor(col, row, Double.NaN)
        }
      } else {
        cfor(0)(_ < colOffset, _ + 1) { col =>
          visitor(col, row, Double.NaN)
        }
        cfor(0)(_ < chunk.cols, _ + 1) { col =>
          visitor(col + colOffset, row, chunk.getDouble(col, row - rowOffset))
        }
        cfor(colOffset + chunk.cols)(_ < cols, _ + 1) { col =>
          visitor(col, row, Double.NaN)
        }
      }
    }
  }

  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    chunk.foreach { (col, row, z) =>
      tile.set(colOffset + col, rowOffset + row, mapper(col, row, z))
    }

    tile
  }

  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    chunk.foreachDouble { (col, row, z) =>
      tile.setDouble(colOffset + col, rowOffset + row, mapper(col, row, z))
    }

    tile
  }
}
