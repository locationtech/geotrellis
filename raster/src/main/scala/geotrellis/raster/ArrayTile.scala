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

import spire.syntax.cfor._

/**
  * [[ArrayTile]] provides access and update to the grid data of a
  * tile.  Designed to be a near drop-in replacement for Array in many
  * cases.
  */
trait ArrayTile extends Tile with Serializable {

  /**
    * Return the [[ArrayTile]] equivalent of this ArrayTile.
    *
    * @return  The object on which the method was invoked
    */
  def toArrayTile = this

  /**
    * Returns a [[Tile]] equivalent to this [[ArrayTile]], except with
    * cells of the given type.
    *
    * @param   targetCellType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(targetCellType: CellType): ArrayTile = {
    val tile = ArrayTile.alloc(targetCellType, cols, rows)

    if(targetCellType.isFloatingPoint != cellType.isFloatingPoint)
      logger.debug(s"Conversion from $cellType to $targetCellType may lead to data loss.")

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

  def withNoData(noDataValue: Option[Double]): Tile

  def interpretAs(newCellType: CellType): Tile

  /**
    * Execute a function on each cell of the [[ArrayTile]].
    *
    * @param  f  A function from Int to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreach(f: Int => Unit): Unit = {
    val len = size
    var i = 0
    while (i < len) {
      f(apply(i))
      i += 1
    }
  }

  /**
    * Execute a function on each cell of the [[ArrayTile]].
    *
    * @param  f  A function from Double to Unit.  Presumably, the function is executed for side-effects.
    */
  def foreachDouble(f: Double => Unit): Unit = {
    val len = size
    var i = 0
    while (i < len) {
      f(applyDouble(i))
      i += 1
    }
  }

  /**
    * Execute an [[IntTileVisitor]] at each cell of the [[ArrayTile]].
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
    * Execute an [[DoubleTileVisitor]] at each cell of the [[ArrayTile]].
    *
    * @param  visitor  A DoubleTileVisitor
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
  def map(f: Int=>Int): Tile = {
    val output = ArrayTile.alloc(cellType, cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output(i) = f(apply(i))
      i += 1
    }
    output
  }

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def mapDouble(f: Double => Double): Tile = {
    val len = size
    val tile = ArrayTile.alloc(cellType, cols, rows)
    var i = 0
    while (i < len) {
      tile.updateDouble(i, f(applyDouble(i)))
      i += 1
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
    * Combine two [[ArrayTile]]s' cells into new cells using the given
    * integer function. For every (x, y) cell coordinate, get each of
    * the ArrayTiles' integer values, map them to a new value, and
    * assign it to the output's (x, y) cell.
    *
    * @param   other  The other ArrayTile
    * @param   f      A function from (Int, Int) to Int
    * @return         The result, an ArrayTile
    */
  def combine(other: ArrayTile)(f: (Int, Int) => Int): ArrayTile = {
    (this, other).assertEqualDimensions

    val output = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output(i) = f(apply(i), other(i))
      i += 1
    }
    output
  }

  /**
    * Combine the cells of an [[ArrayTile]] and a [[Tile]] into a new
    * Tile using the given function. For every (x, y) cell coordinate,
    * get each of the Tiles' integer value, map them to a new value,
    * and assign it to the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Int, Int) to Int
    * @return         The result, an Tile
    */
  def combine(other: Tile)(f: (Int, Int) => Int): Tile =
    other match {
      case ar: ArrayTile =>
        combine(ar)(f)
      case ct: ConstantTile =>
        ct.combine(this)((z1, z2) => f(z2, z1))
      case ct: CompositeTile =>
        ct.combine(this)((z1, z2) => f(z2, z1))
      case ct: CroppedTile =>
        ct.combine(this)((z1, z2) => f(z2, z1))
  }

  /**
    * Combine two [[ArrayTile]]s' cells into new cells using the given
    * double function. For every (x, y) cell coordinate, get each of
    * the ArrayTiles' double values, map them to a new value, and
    * assign it to the output's (x, y) cell.
    *
    * @param   other  The other ArrayTile
    * @param   f      A function from (Double, Double) to Double
    * @return         The result, an ArrayTile
    */
  def combineDouble(other: ArrayTile)(f: (Double, Double) => Double): ArrayTile = {
    (this, other).assertEqualDimensions

    val output = ArrayTile.alloc(cellType.union(other.cellType), cols, rows)
    var i = 0
    val len = size
    while (i < len) {
      output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
      i += 1
    }
    output
  }

  /**
    * Combine the cells of an [[ArrayTile]] and a [[Tile]] into a new
    * Tile using the given function. For every (x, y) cell coordinate,
    * get tiles' double values, map them to a new value, and assign it
    * to the output's (x, y) cell.
    *
    * @param   other  The other Tile
    * @param   f      A function from (Double, Double) to Double
    * @return         The result, an Tile
    */
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = {
    other match {
      case ar: ArrayTile =>
        combineDouble(ar)(f)
      case ct: ConstantTile =>
        ct.combineDouble(this)(f)
      case ct: CompositeTile =>
        ct.combineDouble(this)((z1, z2) => f(z2, z1))
    }
  }

  /**
    * Check for equality between the present [[ArrayTile]] and any
    * other object.
    *
    * @param   other  The other object
    * @return         A boolean
    */
  override def equals(other: Any): Boolean = other match {
    case r: ArrayTile => {
      if (r == null) return false
      val len = size
      if (len != r.size) return false
      var i = 0
      while (i < len) {
        if (apply(i) != r(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Int datum found at the index
    */
  def apply(i: Int): Int

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def applyDouble(i: Int): Double

  /**
    * Fetch the datum at the given column and row of the
    * [[ArrayTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int, row: Int) = apply(row * cols + col)

  /**
    * Fetch the datum at the given column and row of the
    * [[ArrayTile]].
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Double datum found at the given location
    */
  def getDouble(col: Int, row: Int) = applyDouble(row * cols + col)

  /**
    * Return a copy of the present [[ArrayTile]].
    *
    * @return  The copy
    */
  def copy: ArrayTile

  /**
    * Return the underlying array of this [[ArrayTile]] as a list.
    *
    * @return  The list
    */
  def toList = toArray.toList

  /**
    * Return the under-laying array of this [[ArrayTile]] as a list.
    *
    * @return  The list
    */
  def toListDouble = toArrayDouble.toList

  /**
    * Return a copy of the underlying array of the present
    * [[ArrayTile]].
    *
    * @return  The copy as an Array[Int]
    */
  def toArray: Array[Int] = {
    val len = size
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = apply(i)
      i += 1
    }
    arr
  }

  /**
    * Return a copy of the underlying array of the present
    * [[ArrayTile]].
    *
    * @return  The copy as an Array[Double]
    */
  def toArrayDouble: Array[Double] = {
    val len = size
    val arr = Array.ofDim[Double](len)
    var i = 0
    while (i < len) {
      arr(i) = applyDouble(i)
      i += 1
    }
    arr
  }
}

/**
  * An object housing apply methods which produce [[ArrayTile]]s.
  */
object ArrayTile {

  /**
    * Allocate a new [[MutableArrayTile]].
    *
    * @param   t     The [[CellType]] of the new [[MutableArrayTile]]
    * @param   cols  The number of columns that the new [[MutableArrayTile]] should have
    * @param   rows  The number of rows that the new [[MutableArrayTile]] should have
    * @return        The new [[MutableArrayTile]]
    */
  def alloc(t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case _: BitCells => BitArrayTile.ofDim(cols, rows)
      case ct: ByteCells => ByteArrayTile.ofDim(cols, rows, ct)
      case ct: UByteCells => UByteArrayTile.ofDim(cols, rows, ct)
      case ct: ShortCells => ShortArrayTile.ofDim(cols, rows, ct)
      case ct: UShortCells => UShortArrayTile.ofDim(cols, rows, ct)
      case ct: IntCells => IntArrayTile.ofDim(cols, rows, ct)
      case ct: FloatCells => FloatArrayTile.ofDim(cols, rows, ct)
      case ct: DoubleCells => DoubleArrayTile.ofDim(cols, rows, ct)
    }

  /**
    * Create a new, empty [[MutableArrayTile]].
    *
    * @param   t     The [[CellType]] of the new [[MutableArrayTile]]
    * @param   cols  The number of columns that the new [[MutableArrayTile]] should have
    * @param   rows  The number of rows that the new [[MutableArrayTile]] should have
    * @return        The new [[MutableArrayTile]]
    */
  def empty(t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case _: BitCells => BitArrayTile.empty(cols, rows)
      case ct: ByteCells => ByteArrayTile.empty(cols, rows, ct)
      case ct: UByteCells => UByteArrayTile.empty(cols, rows, ct)
      case ct: ShortCells => ShortArrayTile.empty(cols, rows, ct)
      case ct: UShortCells => UShortArrayTile.empty(cols, rows, ct)
      case ct: IntCells => IntArrayTile.empty(cols, rows, ct)
      case ct: FloatCells => FloatArrayTile.empty(cols, rows, ct)
      case ct: DoubleCells => DoubleArrayTile.empty(cols, rows, ct)
    }

  /**
    * Create a [[MutableArrayTile]] from a byte array.
    *
    * @param   bytes  The array of bytes
    * @param   t      The [[CellType]] of the new [[MutableArrayTile]]
    * @param   cols   The number of columns that the new [[MutableArrayTile]] should have
    * @param   rows   The number of rows that the new [[MutableArrayTile]] should have
    * @return         The new [[MutableArrayTile]]
    */
  def fromBytes(bytes: Array[Byte], t: CellType, cols: Int, rows: Int): MutableArrayTile =
    t match {
      case _: BitCells => BitArrayTile.fromBytes(bytes, cols, rows)
      case ct: ByteCells => ByteArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: UByteCells => UByteArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: ShortCells => ShortArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: UShortCells => UShortArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: IntCells => IntArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: FloatCells => FloatArrayTile.fromBytes(bytes, cols, rows, ct)
      case ct: DoubleCells => DoubleArrayTile.fromBytes(bytes, cols, rows, ct)
    }

  /**
    * Create a new [[ByteConstantNoDataArrayTile]] from an array of
    * Bytes.
    *
    * @param   arr   The array of Bytes
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int): ByteConstantNoDataArrayTile = new ByteConstantNoDataArrayTile(arr, cols, rows)

  /**
    * Create a new [[ShortConstantNoDataArrayTile]] from an array of
    * Shorts.
    *
    * @param   arr   The array of Shorts
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int): ShortConstantNoDataArrayTile = new ShortConstantNoDataArrayTile(arr, cols, rows)

  /**
    * Create a new [[IntConstantNoDataArrayTile]] from an array of
    * integers.
    *
    * @param   arr   The array of integers
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int): IntConstantNoDataArrayTile = new IntConstantNoDataArrayTile(arr, cols, rows)

  /**
    * Create a new [[FloatConstantNoDataArrayTile]] from an array of
    * Floats.
    *
    * @param   arr   The array of Floats
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int): FloatConstantNoDataArrayTile = new FloatConstantNoDataArrayTile(arr, cols, rows)

  /**
    * Create a new [[DoubleConstantNoDataArrayTile]] from an array of
    * Doubles.
    *
    * @param   arr   The array of Doubles
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int): DoubleConstantNoDataArrayTile = new DoubleConstantNoDataArrayTile(arr, cols, rows)
}

/**
  * An object housing apply methods which produce [[RawArrayTile]]s.
  */
object RawArrayTile {

  /**
    * Create a new [[ByteRawArrayTile]] from an array of Bytes.
    *
    * @param   arr   The array of Bytes
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int): ByteRawArrayTile = new ByteRawArrayTile(arr, cols, rows)

  /**
    * Create a new [[ShortRawArrayTile]] from an array of Shorts.
    *
    * @param   arr   The array of Shorts
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int): ShortRawArrayTile = new ShortRawArrayTile(arr, cols, rows)

  /**
    * Create a new [[IntRawArrayTile]] from an array of integers.
    *
    * @param   arr   The array of integers
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int): IntRawArrayTile = new IntRawArrayTile(arr, cols, rows)

  /**
    * Create a new [[FloatRawArrayTile]] from an array of Floats.
    *
    * @param   arr   The array of Floats
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int): FloatRawArrayTile = new FloatRawArrayTile(arr, cols, rows)

  /**
    * Create a new [[DoubleRawArrayTile]] from an array of Doubles.
    *
    * @param   arr   The array of Doubles
    * @param   cols  The number of columns in the new tile
    * @param   rows  The number of rows in the new tile
    * @return        The newly-created tile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int): DoubleRawArrayTile = new DoubleRawArrayTile(arr, cols, rows)
}
