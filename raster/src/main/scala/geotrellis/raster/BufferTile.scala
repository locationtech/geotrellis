/*
 * Copyright 2021 Azavea
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

import geotrellis.raster.mapalgebra.focal.BufferTileFocalMethods
import spire.syntax.cfor._

/**
 * When combined with another BufferTile the two tiles will be aligned on (0, 0) pixel of tile center.
 * The operation will be carried over all overlapping pixels.
 * For instance: combining a tile padded with 5 pixels on all sides with tile padded with 3 pixels on all sides will
 * result in buffer tile with 3 pixel padding on all sides.
 *
 * When combined with another BufferTile the operation will be executed over the maximum shared in
 *
 * Behaves like a regular tile within the gridBounds. The access to the buffer is achieved through the direct
 * access to the sourceTile or via the mapTile function that maps over the sourceTile.
 */
case class BufferTile(
  sourceTile: Tile,
  gridBounds: GridBounds[Int]
) extends Tile {
  require(
    gridBounds.colMin >=0 &&
    gridBounds.rowMin >= 0 &&
    gridBounds.colMax < sourceTile.cols &&
    gridBounds.rowMax < sourceTile.rows,
    s"Tile center bounds $gridBounds exceed underlying tile dimensions ${sourceTile.dimensions}"
  )

  val cols: Int = gridBounds.width
  val rows: Int = gridBounds.height

  val cellType: CellType = sourceTile.cellType

  private def colMin: Int = gridBounds.colMin
  private def rowMin: Int = gridBounds.rowMin
  private def sourceCols: Int = sourceTile.cols
  private def sourceRows: Int = sourceTile.rows

  def bufferTop: Int = gridBounds.rowMin
  def bufferLeft: Int = gridBounds.colMin
  def bufferRight: Int = sourceTile.cols - gridBounds.colMin - gridBounds.colMax
  def bufferBottom: Int = sourceTile.rows - gridBounds.rowMin - gridBounds.rowMax

  /**
    * Returns a [[Tile]] equivalent to this tile, except with cells of
    * the given type.
    *
    * @param   targetCellType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(targetCellType: CellType): Tile =
    mutable(targetCellType)

  def withNoData(noDataValue: Option[Double]): BufferTile =
    BufferTile(sourceTile.withNoData(noDataValue), gridBounds)

  def interpretAs(newCellType: CellType): BufferTile =
    BufferTile(sourceTile.interpretAs(newCellType), gridBounds)

  /**
    * Fetch the datum at the given column and row of the tile.
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int, row: Int): Int = {
    val c = col + colMin
    val r = row + rowMin
    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      throw new IndexOutOfBoundsException(s"(col=$col, row=$row) is out of tile bounds")
    } else {
      sourceTile.get(c, r)
    }
  }

  /**
    * Fetch the datum at the given column and row of the tile.
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Double datum found at the given location
    */
  def getDouble(col: Int, row: Int): Double = {
    val c = col + colMin
    val r = row + rowMin

    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      throw new IndexOutOfBoundsException(s"(col=$col, row=$row) is out of tile bounds")
    } else {
      sourceTile.getDouble(col + gridBounds.colMin, row + gridBounds.rowMin)
    }
  }

  /**
    * Another name for the 'mutable' method on this class.
    *
    * @return  An [[ArrayTile]]
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  An MutableArrayTile
    */
  def mutable: MutableArrayTile =
    mutable(cellType)

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  An MutableArrayTile
    */
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

  /**
    * Return the data behind this tile as an array of integers.
    *
    * @return  The copy as an Array[Int]
    */
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

  /**
    * Return the data behind this tile as an array of doubles.
    *
    * @return  The copy as an Array[Int]
    */
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

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = toArrayTile().toBytes()

  /**
    * Execute a function on each cell of the tile.  The function
    * returns Unit, so it presumably produces side-effects.
    *
    * @param  f  A function from Int to Unit
    */
  def foreach(f: Int => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        f(get(col, row))
      }
    }
  }

  /**
    * Execute a function on each cell of the tile.  The function
    * returns Unit, so it presumably produces side-effects.
    *
    * @param  f  A function from Double to Unit
    */
  def foreachDouble(f: Double => Unit): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        f(getDouble(col, row))
      }
    }
  }

  /**
    * Execute an [[IntTileVisitor]] at each cell of the present tile.
    *
    * @param  visitor  An IntTileVisitor
    */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, get(col, row))
      }
    }
  }

  /**
    * Execute an [[DoubleTileVisitor]] at each cell of the present tile.
    *
    * @param  visitor  An DoubleTileVisitor
    */
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, getDouble(col, row))
      }
    }
  }

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Int to Int, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row)))
      }
    }

    tile
  }

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def mapDouble(f: Double => Double): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row)))
      }
    }

    tile
  }

  /**
    * Map an [[IntTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, mapper(col, row, get(col, row)))
      }
    }
    tile
  }

  /**
    * Map an [[DoubleTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, a [[Tile]]
    */
  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, mapper(col, row, getDouble(col, row)))
      }
    }
    tile
  }

  def combine(other: BufferTile)(f: (Int, Int) => Int): Tile = {
    if ((this.gridBounds.width != other.gridBounds.width) || (this.gridBounds.height != other.gridBounds.height))
      throw GeoAttrsError("Cannot combine rasters with different dimensions: " +
        s"${this.gridBounds.width}x${this.gridBounds.height} != ${other.gridBounds.width}x${other.gridBounds.height}")

    val bufferTop = math.min(this.bufferTop, other.bufferTop)
    val bufferLeft = math.min(this.bufferLeft, other.bufferLeft)
    val bufferRight = math.min(this.bufferRight, other.bufferRight)
    val bufferBottom = math.min(this.bufferBottom, other.bufferBottom)
    val cols = bufferLeft + gridBounds.width + bufferRight
    val rows = bufferTop + gridBounds.height + bufferBottom

    val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)

    // index both tiles relative to (0, 0) pixel
    cfor(-bufferTop)(_ < gridBounds.height + bufferRight, _ + 1) { row =>
      cfor(-bufferLeft)(_ < gridBounds.width + bufferRight, _ + 1) { col =>
        val leftV = this.get(col, row)
        val rightV = other.get(col, row)
        tile.set(col + bufferLeft, row + bufferTop, f(leftV, rightV))
      }
    }

    if (bufferTop + bufferLeft + bufferRight + bufferBottom == 0) tile
    else BufferTile(tile, GridBounds[Int](
      colMin = bufferLeft,
      rowMin = bufferTop,
      colMax = bufferLeft + gridBounds.width - 1,
      rowMax = bufferTop + gridBounds.height - 1)
    )
  }

  def combineDouble(other: BufferTile)(f: (Double, Double) => Double): Tile = {
    if ((this.gridBounds.width != other.gridBounds.width) || (this.gridBounds.height != other.gridBounds.height))
      throw GeoAttrsError("Cannot combine rasters with different dimensions: " +
        s"${this.gridBounds.width}x${this.gridBounds.height} != ${other.gridBounds.width}x${other.gridBounds.height}")

    val bufferTop = math.min(this.bufferTop, other.bufferTop)
    val bufferLeft = math.min(this.bufferLeft, other.bufferLeft)
    val bufferRight = math.min(this.bufferRight, other.bufferRight)
    val bufferBottom = math.min(this.bufferBottom, other.bufferBottom)
    val cols = bufferLeft + gridBounds.width + bufferRight
    val rows = bufferTop + gridBounds.height + bufferBottom

    val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)

    // index both tiles relative to (0, 0) pixel
    cfor(-bufferTop)(_ < gridBounds.height + bufferRight, _ + 1) { row =>
      cfor(-bufferLeft)(_ < gridBounds.width + bufferRight, _ + 1) { col =>
        val leftV = this.getDouble(col, row)
        val rightV = other.getDouble(col, row)
        tile.setDouble(col + bufferLeft, row + bufferTop, f(leftV, rightV))
      }
    }

    if (bufferTop + bufferLeft + bufferRight + bufferBottom == 0) tile
    else BufferTile(tile, GridBounds[Int](
      colMin = bufferLeft,
      rowMin = bufferTop,
      colMax = bufferLeft + gridBounds.width - 1,
      rowMax = bufferTop + gridBounds.height - 1)
    )
  }

  /**
    * Combine two tiles' cells into new cells using the given integer
    * function. For every (x, y) cell coordinate, get each of the
    * tiles' integer values, map them to a new value, and assign it to
    * the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Int, Int) to Int
    * @return         The result, an Tile
    */
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions()

    other match {
      case bt: BufferTile =>
        this.combine(bt)(f)
      case _ =>
        val tile = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            tile.set(col, row, f(get(col, row), other.get(col, row)))
          }
        }
        tile
    }
  }

  /**
    * Combine two tiles' cells into new cells using the given double
    * function. For every (x, y) cell coordinate, get each of the
    * tiles' double values, map them to a new value, and assign it to
    * the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Int, Int) to Int
    * @return         The result, an Tile
    */
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions()

    other match {
      case bt: BufferTile =>
        this.combineDouble(bt)(f)
      case _ =>
        val tile = ArrayTile.alloc(cellType, cols, rows)
        cfor(0)(_ < rows, _ + 1) { row =>
          cfor(0)(_ < cols, _ + 1) { col =>
            tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
          }
        }
        tile
    }
  }

  def mapTile(f: Tile => Tile): BufferTile = BufferTile(f(sourceTile), gridBounds)

  override def toString: String = s"BufferTile(${sourceTile.dimensions}, $gridBounds, $cellType)"
}

object BufferTile {
  /**
   * This implicit class is not a part of the [[geotrellis.raster.mapalgebra.focal.Implicits]]
   * to disambiguate implicit classes application.
   */
  implicit class withBufferTileFocalMethods(val self: BufferTile) extends BufferTileFocalMethods
}
