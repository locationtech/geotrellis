/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import java.lang.IllegalArgumentException
import java.nio.ByteBuffer

import spire.syntax.cfor._


/**
  * The trait underlying constant tile types.
  */
trait ConstantTile extends Tile {
  protected val iVal: Int
  protected val dVal: Double

  /**
    * Fetch the datum at the given column and row of the tile.
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Int datum found at the given location
    */
  def get(col: Int, row: Int): Int = iVal

  /**
    * Fetch the datum at the given column and row of the tile.
    *
    * @param   col  The column
    * @param   row  The row
    * @return       The Double datum found at the given location
    */
  def getDouble(col: Int, row: Int): Double = dVal

  /**
    * Return the data behind this tile as an array of integers.
    *
    * @return  The copy as an Array[Int]
    */
  def toArray(): Array[Int] = Array.ofDim[Int](cols * rows).fill(iVal)

  /**
    * Return the data behind this tile as an array of doubles.
    *
    * @return  The copy as an Array[Int]
    */
  def toArrayDouble(): Array[Double] = Array.ofDim[Double](cols * rows).fill(dVal)

  /**
    * Returns a [[Tile]] equivalent to this tile, except with cells of
    * the given type.
    *
    * @param   cellType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(newType: CellType): Tile =
    newType match {
      case BitCellType => BitConstantTile(if(iVal == 0) false else true, cols, rows)
      case ByteConstantNoDataCellType => ByteConstantTile(i2b(iVal), cols, rows)
      case UByteConstantNoDataCellType => UByteConstantTile(i2ub(iVal), cols, rows)
      case ShortConstantNoDataCellType => ShortConstantTile(i2s(iVal), cols, rows)
      case UShortConstantNoDataCellType => UShortConstantTile(i2us(iVal), cols, rows)
      case IntConstantNoDataCellType => IntConstantTile(iVal, cols, rows)
      case FloatConstantNoDataCellType => FloatConstantTile(d2f(dVal), cols, rows)
      case DoubleConstantNoDataCellType => DoubleConstantTile(dVal, cols, rows)
      case _ => throw new IllegalArgumentException(s"Unsupported ConstantTile CellType: $newType")
    }

  /**
    * Execute a function on each cell of the tile.  The function
    * returns Unit, so it presumably produces side-effects.
    *
    * @param  f  A function from Int to Unit
    */
  def foreach(f: Int => Unit) {
    var i = 0
    val len = size
    while (i < len) { f(iVal); i += 1 }
  }

  /**
    * Execute a function on each cell of the tile.  The function
    * returns Unit, so it presumably produces side-effects.
    *
    * @param  f  A function from Double to Unit
    */
  def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = size
    while (i < len) { f(dVal); i += 1 }
  }

  /**
    * Execute an [[IntTileVisitor]] at each cell of the present tile.
    *
    * @param  visitor  An IntTileVisitor
    */
  def foreachIntVisitor(visitor: IntTileVisitor): Unit = {
    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        visitor(col, row, iVal)
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
        visitor(col, row, dVal)
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
  def map(f: Int => Int): Tile = IntConstantTile(f(iVal), cols, rows)

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
  def combine(other: Tile)(f: (Int, Int) => Int): Tile = other.map(z => f(iVal, z))

  /**
    * Map each cell in the given tile to a new one, using the given
    * function.
    *
    * @param   f  A function from Double to Double, executed at each point of the tile
    * @return     The result, a [[Tile]]
    */
  def mapDouble(f: Double => Double): Tile = DoubleConstantTile(f(dVal), cols, rows)

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
  def combineDouble(other: Tile)(f: (Double, Double) => Double): Tile = other.mapDouble(z => f(dVal, z))

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
}

/**
  * The companion object for the [[BitConstantTile]] type.
  */
object BitConstantTile {

  /**
    * A function which takes a number of colums and rows, and produces
    * a new BitConstantTile.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The BitConstantTile
    */
  def apply(i: Int, cols: Int, rows: Int): BitConstantTile =
    BitConstantTile(if(i == 0) false else true, cols, rows)
}

/**
  * The [[BitConstantTile]] type.
  */
case class BitConstantTile(v: Boolean, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = if(v) 1 else 0
  protected val dVal = if(v) 1.0 else 0.0

  val cellType = BitCellType

  /**
    * Another name for the 'mutable' method on this class.
    *
    * @return  An [[ArrayTile]]
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = BitArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = Array(iVal.toByte)
}

/**
  * The [[ByteConstantTile]] type.
  */
case class ByteConstantTile(v: Byte, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = b2i(v)
  protected val dVal = b2d(v)

  val cellType = ByteConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = ByteArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = Array(v)
}

/**
  * The [[UByteConstantTile]] type.
  */
case class UByteConstantTile(v: Byte, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = ub2i(v)
  protected val dVal = ub2d(v)

  val cellType = ByteConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = ByteArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = Array(v)

  /**
    * Resample this [[ByteConstantTile]] to a target extent.
    *
    * @param   current  An extent (ignored)
    * @param   target   The target extent
    * @param   method   The resampling method (ignored)
    * @return           The resampled [[Tile]]
    */
  def resample(current: Extent, target: RasterExtent, method: ResampleMethod): Tile =
    ByteConstantTile(v, target.cols, target.rows)
}

/**
  * The [[ShortConstantTile]] type.
  */
case class ShortConstantTile(v: Short, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = s2i(v)
  protected val dVal = s2d(v)

  val cellType = ShortConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = ShortArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asShortBuffer.put(v)
    arr
  }
}

/**
  * The [[UShortConstantTile]] type.
  */
case class UShortConstantTile(v: Short, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = us2i(v)
  protected val dVal = us2d(v)

  val cellType = ShortConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = UShortArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asShortBuffer.put(v)
    arr
  }

  /**
    * Resample this [[ShortConstantTile]] to a target extent.
    *
    * @param   current  An extent (ignored)
    * @param   target   The target extent
    * @param   method   The resampling method (ignored)
    * @return           The resampled [[Tile]]
    */
  def resample(current: Extent, target: RasterExtent, method: ResampleMethod): Tile =
    ShortConstantTile(v, target.cols, target.rows)
}

/**
  * The [[IntConstantTile]] type.
  */
case class IntConstantTile(v: Int, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = v
  protected val dVal = i2d(v)

  val cellType = IntConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = IntArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asIntBuffer.put(v)
    arr
  }
}

/**
  * The [[FloatConstantTile]] type.
  */
case class FloatConstantTile(v: Float, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = f2i(v)
  protected val dVal = f2d(v)

  val cellType = FloatConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = FloatArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asFloatBuffer.put(v)
    arr
  }
}

/**
  * The [[DoubleConstantTile]] type.
  */
case class DoubleConstantTile(v: Double, cols: Int, rows: Int) extends ConstantTile {
  protected val iVal = d2i(v)
  protected val dVal = v

  val cellType = DoubleConstantNoDataCellType

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable(): MutableArrayTile = DoubleArrayTile.fill(dVal, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asDoubleBuffer.put(dVal)
    arr
  }
}
