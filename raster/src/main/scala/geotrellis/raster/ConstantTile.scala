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

import java.nio.ByteBuffer

import spire.syntax.cfor._


/**
  * The trait underlying constant tile types.
  */
trait ConstantTile extends Tile {

  /** Precomputed view of tile cells as seen by [[get]] method */
  protected val iVal: Int

  /** Precomputed view of tile cells as seen by [[getDouble]] method */
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
    * @param   newType  The type of cells that the result should have
    * @return            The new Tile
    */
  def convert(newType: CellType): Tile = {
    if(newType.isFloatingPoint != cellType.isFloatingPoint)
      logger.warn(s"Conversion from $cellType to $newType may lead to data loss.")

    newType match {
      case BitCellType => new BitConstantTile(if (iVal == 0) false else true, cols, rows)
      case ct: ByteCells => ByteConstantTile(iVal.toByte, cols, rows, ct)
      case ct: UByteCells => UByteConstantTile(iVal.toByte, cols, rows, ct)
      case ct: ShortCells => ShortConstantTile(iVal.toShort , cols, rows, ct)
      case ct: UShortCells =>  UShortConstantTile(iVal.toShort , cols, rows, ct)
      case ct: IntCells =>  IntConstantTile(iVal , cols, rows, ct)
      case ct: FloatCells => FloatConstantTile(dVal.toFloat , cols, rows, ct)
      case ct: DoubleCells => DoubleConstantTile(dVal, cols, rows, ct)
    }
  }

  def interpretAs(newCellType: CellType): Tile =
    withNoData(None).convert(newCellType)

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
  def map(f: Int => Int): Tile

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
  def mapDouble(f: Double => Double): Tile

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

object ConstantTile {
  /**
   * Create a [[ConstantTile]] from a byte array.
   *
   * @param   bytes  The array of bytes, as provided by [[ConstantTile.toBytes()]]
   * @param   t      The [[CellType]] of the new [[ConstantTile]]
   * @param   cols   The number of columns that the new [[ConstantTile]] should have
   * @param   rows   The number of rows that the new [[ConstantTile]] should have
   * @return         The new [[ConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], t: CellType, cols: Int, rows: Int): ConstantTile =
    t match {
      case _: BitCells => BitConstantTile.fromBytes(bytes, cols, rows)
      case ct: ByteCells => ByteConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: UByteCells => UByteConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: ShortCells => ShortConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: UShortCells => UShortConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: IntCells => IntConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: FloatCells => FloatConstantTile.fromBytes(bytes, cols, rows, ct)
      case ct: DoubleCells => DoubleConstantTile.fromBytes(bytes, cols, rows, ct)
    }
}

/**
  * The companion object for the [[BitConstantTile]] type.
  */
object BitConstantTile {

  /**
    * A function which takes a number of columns and rows, and produces
    * a new BitConstantTile.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The BitConstantTile
    */
  def apply(i: Int, cols: Int, rows: Int): BitConstantTile =
    BitConstantTile(if(i == 0) false else true, cols, rows)

  /**
   * Produce a new [[BitConstantTile]] from an array of bytes.
   *
   * @param   bytes  The value to fill into the new tile
   * @param   cols   The number of columns
   * @param   rows   The number of rows
   * @return         The new BitArrayTile
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): BitConstantTile = {
    val interpreted = BitArrayTile.fromBytes(bytes, 1, 1)
    BitConstantTile(interpreted.array(0), cols, rows)
  }
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
  def mutable: MutableArrayTile = BitArrayTile.fill(v, cols, rows)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = Array(iVal.toByte)

  def withNoData(noDataValue: Option[Double]): ConstantTile  =  this

  def map(f: Int => Int): Tile = BitConstantTile(f(iVal), cols, rows)

  def mapDouble(f: Double => Double): Tile = BitConstantTile(d2i(f(dVal)), cols, rows)
}

/**
  * The [[ByteConstantTile]] type.
  */
case class ByteConstantTile(v: Byte, cols: Int, rows: Int,
  cellType: ByteCells with NoDataHandling = ByteConstantNoDataCellType
) extends ConstantTile {
  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (b2i(v), b2d(v))
      case _: NoNoData =>
        (v.toInt, v.toDouble)
      case ct: ByteUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */

  def mutable: MutableArrayTile = ByteArrayTile.fill(v, cols, rows, cellType)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = Array(v)

  def withNoData(noDataValue: Option[Double]) =
    ByteConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = ByteConstantTile(i2b(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = ByteConstantTile(d2b(f(dVal)), cols, rows)
}

object ByteConstantTile {
  /**
   * Create a new [[ByteConstantTile]] from the array of bytes
   * produced by [[ByteConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[ByteConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: ByteCells with NoDataHandling): ByteConstantTile = {
    val interpreted = ByteArrayTile.fromBytes(bytes, 1, 1, cellType)
    ByteConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}

/**
  * The [[UByteConstantTile]] type.
  */
case class UByteConstantTile(v: Byte, cols: Int, rows: Int,
  cellType: UByteCells with NoDataHandling = UByteConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (ub2i(v), ub2d(v))
      case _: NoNoData =>
        (v.toInt, v.toDouble)
      case ct: UByteUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = UByteArrayTile.fill(v, cols, rows, cellType)


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

  def withNoData(noDataValue: Option[Double]) =
    UByteConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = UByteConstantTile(i2ub(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = UByteConstantTile(d2ub(f(dVal)), cols, rows)
}

object UByteConstantTile {
  /**
   * Create a new [[UByteConstantTile]] from the array of bytes
   * produced by [[UByteConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[UByteConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: UByteCells with NoDataHandling): UByteConstantTile = {
    val interpreted = UByteArrayTile.fromBytes(bytes, 1, 1, cellType)
    UByteConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}

/**
  * The [[ShortConstantTile]] type.
  */
case class ShortConstantTile(v: Short, cols: Int, rows: Int,
  cellType: ShortCells with NoDataHandling = ShortConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (s2i(v), s2d(v))
      case _: NoNoData =>
        (v.toInt, v.toDouble)
      case ct: ShortUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = ShortArrayTile.fill(v, cols, rows, cellType)


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

  def withNoData(noDataValue: Option[Double]) =
    ShortConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = ShortConstantTile(i2s(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = ShortConstantTile(d2s(f(dVal)), cols, rows)
}

object ShortConstantTile {
  /**
   * Create a new [[ShortConstantTile]] from the array of bytes
   * produced by [[ShortConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[ShortConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: ShortCells with NoDataHandling): ShortConstantTile = {
    val interpreted = ShortArrayTile.fromBytes(bytes, 1, 1, cellType)
    ShortConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}

/**
  * The [[UShortConstantTile]] type.
  */
case class UShortConstantTile(v: Short, cols: Int, rows: Int,
  cellType: UShortCells with NoDataHandling = UShortConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (us2i(v), us2d(v))
      case _: NoNoData =>
        (v.toInt, v.toDouble)
      case ct: UShortUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = UShortArrayTile.fill(v, cols, rows, cellType)

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

  def withNoData(noDataValue: Option[Double]) =
    UShortConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = UShortConstantTile(i2us(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = UShortConstantTile(d2us(f(dVal)), cols, rows)
}

object UShortConstantTile {
  /**
   * Create a new [[UShortConstantTile]] from the array of bytes
   * produced by [[UShortConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[UShortConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: UShortCells with NoDataHandling): UShortConstantTile = {
    val interpreted = UShortArrayTile.fromBytes(bytes, 1, 1, cellType)
    UShortConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}

/**
  * The [[IntConstantTile]] type.
  */
case class IntConstantTile(v: Int, cols: Int, rows: Int,
  cellType: IntCells with NoDataHandling = IntConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (v, i2d(v))
      case _: NoNoData =>
        (v, v.toDouble)
      case ct: IntUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = IntArrayTile.fill(v, cols, rows, cellType)

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

  def withNoData(noDataValue: Option[Double]) =
    IntConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = IntConstantTile(f(iVal), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = IntConstantTile(d2i(f(dVal)), cols, rows)
}

object IntConstantTile {
  /**
   * Create a new [[IntConstantTile]] from the array of bytes
   * produced by [[IntConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[IntConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: IntCells with NoDataHandling): IntConstantTile = {
    val interpreted = IntArrayTile.fromBytes(bytes, 1, 1, cellType)
    IntConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}

/**
  * The [[FloatConstantTile]] type.
  */
case class FloatConstantTile(v: Float, cols: Int, rows: Int,
  cellType: FloatCells with NoDataHandling = FloatConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (f2i(v), f2d(v))
      case _: NoNoData =>
        (v.toInt, v.toDouble)
      case ct: FloatUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v.toDouble)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = FloatArrayTile.fill(v, cols, rows, cellType)

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

  def withNoData(noDataValue: Option[Double]) =
    FloatConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = FloatConstantTile(i2f(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = FloatConstantTile(d2f(f(dVal)), cols, rows)
}

object FloatConstantTile {
  /**
   * Create a new [[FloatConstantTile]] from the array of bytes
   * produced by [[FloatConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[FloatConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: FloatCells with NoDataHandling): FloatConstantTile = {
    val interpreted = FloatArrayTile.fromBytes(bytes, 1, 1, cellType)
    FloatConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}


/**
  * The [[DoubleConstantTile]] type.
  */
case class DoubleConstantTile(v: Double, cols: Int, rows: Int,
  cellType: DoubleCells with NoDataHandling = DoubleConstantNoDataCellType
) extends ConstantTile {

  protected val (iVal: Int, dVal: Double) =
    cellType match {
      case _: ConstantNoData[_] =>
        (d2i(v), v)
      case _: NoNoData =>
        (v.toInt, v)
      case ct: DoubleUserDefinedNoDataCellType =>
        if (ct.noDataValue == v) (Int.MinValue, Double.NaN)
        else (v.toInt, v)
    }

  /**
    * Another name for the 'mutable' method on this class.
    */
  def toArrayTile(): ArrayTile = mutable

  /**
    * Return the [[MutableArrayTile]] equivalent of this tile.
    *
    * @return  The MutableArrayTile
    */
  def mutable: MutableArrayTile = DoubleArrayTile.fill(dVal, cols, rows, cellType)

  /**
    * Return the underlying data behind this tile as an array.
    *
    * @return  An array of bytes
    */
  def toBytes(): Array[Byte] = {
    val arr = Array.ofDim[Byte](cellType.bytes)
    ByteBuffer.wrap(arr).asDoubleBuffer.put(v)
    arr
  }

  def withNoData(noDataValue: Option[Double]): ConstantTile =
    DoubleConstantTile(v, cols, rows, cellType.withNoData(noDataValue))

  def map(f: Int => Int): Tile = DoubleConstantTile(i2d(f(iVal)), cols, rows, cellType)

  def mapDouble(f: Double => Double): Tile = DoubleConstantTile(f(dVal), cols, rows)
}

object DoubleConstantTile {
  /**
   * Create a new [[DoubleConstantTile]] from the array of bytes
   * produced by [[DoubleConstantTile.toBytes()]].
   *
   * @param   bytes    The value to fill into the new tile
   * @param   cols     The number of columns
   * @param   rows     The number of rows
   * @param   cellType The cell type from which to derive the NODATA
   * @return           The new [[DoubleConstantTile]]
   */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int,
    cellType: DoubleCells with NoDataHandling): DoubleConstantTile = {
    val interpreted = DoubleArrayTile.fromBytes(bytes, 1, 1, cellType)
    DoubleConstantTile(interpreted.array(0), cols, rows, cellType)
  }
}
