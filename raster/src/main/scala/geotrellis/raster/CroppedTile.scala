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

package geotrellis.raster

import geotrellis.raster.resample._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import scala.collection.mutable


/**
  * The companion object for the [[CroppedTile]] type.
  */
object CroppedTile {

  /**
    * A function which produces a [[CroppedTile]] given a source
    * [[Tile]], a source Extent and a target Extent.
    *
    * @param   sourceTile    The source tile
    * @param   sourceExtent  The extent of the source tile
    * @param   targetExtent  The extent of the newly-created CroppedTile
    * @return                The CroppedTile
    */
  def apply(sourceTile: Tile,
            sourceExtent: Extent,
            targetExtent: Extent): CroppedTile =
    CroppedTile(
      sourceTile,
      RasterExtent(
        sourceExtent,
        sourceTile.cols,
        sourceTile.rows
      ).gridBoundsFor(targetExtent)
    )
}

/**
  * The [[CroppedTile]] type.
  */
case class CroppedTile(
  sourceTile: Tile,
  gridBounds: GridBounds[Int]
) extends Tile {

  val cols = gridBounds.width
  val rows = gridBounds.height

  val cellType = sourceTile.cellType

  private val colMin = gridBounds.colMin
  private val rowMin = gridBounds.rowMin
  private val sourceCols = sourceTile.cols
  private val sourceRows = sourceTile.rows

  /**
    * Returns a [[Tile]] equivalent to this tile, except with cells of
    * the given type.
    *
    * @param   targetCellType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(targetCellType: CellType): Tile =
    mutable(targetCellType)

  def withNoData(noDataValue: Option[Double]): CroppedTile =
    CroppedTile(sourceTile.withNoData(noDataValue), gridBounds)

  def interpretAs(newCellType: CellType): CroppedTile =
    CroppedTile(sourceTile.interpretAs(newCellType), gridBounds)

  /**
    * Fetch the datum at the given column and row of the tile.
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int, row: Int): Int = {
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin
    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      NODATA
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
    val c = col + gridBounds.colMin
    val r = row + gridBounds.rowMin

    if(c < 0 || r < 0 || c >= sourceCols || r >= sourceRows) {
      Double.NaN
    } else {
      sourceTile.getDouble(col + gridBounds.colMin, row + gridBounds.rowMin)
    }
  }

  /**
    * Another name for the 'mutable' method on this class.
    *
    * @return  An [[ArrayTile]]
    */
  def toArrayTile: ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  An MutableArrayTile
    */
  def mutable(): MutableArrayTile =
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
  def toArray: Array[Int] = {
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
  def toArrayDouble: Array[Double] = {
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
  def toBytes(): Array[Byte] = toArrayTile.toBytes

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
    val tile = ArrayTile.alloc(cellType, cols, rows)

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
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.set(col, row, f(get(col, row), other.get(col, row)))
      }
    }

    tile
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
    (this, other).assertEqualDimensions

    val tile = ArrayTile.alloc(cellType, cols, rows)
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        tile.setDouble(col, row, f(getDouble(col, row), other.getDouble(col, row)))
      }
    }

    tile
  }
}
