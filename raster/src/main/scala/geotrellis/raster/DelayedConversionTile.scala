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

/**
  * [DelayedConversionTile]] represents a tile that wraps an inner tile,
  * and for any operation that returns a Tile, returns an ArrayTile with
  * a cell type of the target cell type.
  *
  * @note          get and getDouble don't do what you think you do.
  */
class DelayedConversionTile(inner: Tile, targetCellType: CellType)
  extends Tile {

  val cols = inner.cols
  val rows = inner.rows

  def cellType: CellType =
    inner.cellType

  def get(col: Int, row: Int): Int =
    inner.get(col, row)

  def getDouble(col: Int, row: Int): Double =
    inner.getDouble(col, row)

  def convert(cellType: CellType): Tile =
    inner.convert(cellType)

  def withNoData(noDataValue: Option[Double]): Tile =
    inner.withNoData(noDataValue)

  def interpretAs(newCellType: CellType): Tile =
    withNoData(None).convert(newCellType)

  def toArray = inner.toArray

  def toArrayDouble = inner.toArrayDouble

  def toArrayTile: ArrayTile = mutable

  def mutable: MutableArrayTile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    if(!cellType.isFloatingPoint) {
      inner.foreach { (col, row, z) =>
        tile.set(col, row, z)
      }
    } else {
      inner.foreachDouble { (col, row, z) =>
        tile.setDouble(col, row, z)
      }
    }

    tile
  }

  def toBytes(): Array[Byte] = toArrayTile.toBytes
  def foreach(f: Int => Unit): Unit = inner.foreach(f)
  def foreachDouble(f: Double => Unit): Unit = inner.foreachDouble(f)
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = inner.foreachIntVisitor(visitor)
  def foreachDoubleVisitor(visitor: DoubleTileVisitor): Unit = inner.foreachDoubleVisitor(visitor)

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Int to Int, executed at each point of the tile
    * @return     The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def map(f: Int => Int): Tile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreach { (col, row, z) =>
      tile.set(col, row, f(z))
    }

    tile
  }

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def mapDouble(f: Double =>Double): Tile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreachDouble { (col, row, z) =>
      tile.setDouble(col, row, f(z))
    }

    tile
  }

  /**
    * Map an [[IntTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def mapIntMapper(mapper: IntTileMapper): Tile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreach { (col, row, z) =>
      tile.set(col, row, mapper(col, row, z))
    }

    tile
  }

  /**
    * Map an [[DoubleTileMapper]] over the present tile.
    *
    * @param   mapper  The mapper
    * @return          The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def mapDoubleMapper(mapper: DoubleTileMapper): Tile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreachDouble { (col, row, z) =>
      tile.setDouble(col, row, mapper(col, row, z))
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
    * @return         The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = {
    (this, other).assertEqualDimensions
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreach { (col, row, z) =>
      tile.set(col, row, f(z, other.get(col, row)))
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
    * @return         The result, an [[ArrayTile]] with the target [[CellType]] of this DelayedConversionTile.
    */
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    (this, other).assertEqualDimensions
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    inner.foreachDouble { (col, row, z) =>
      tile.setDouble(col, row, f(z, other.getDouble(col, row)))
    }

    tile
  }
}
