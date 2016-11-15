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

import java.nio.ByteBuffer

/**
 * [[ArrayTile]] based on Array[Double] (each cell as a Double).
 */
abstract class DoubleArrayTile(val array: Array[Double], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: DoubleCells with NoDataHandling

  override def toArrayDouble = array.clone

  /**
    * Convert the present [[DoubleArrayTile]] to an array of bytes and
    * return that array.
    *
    * @return  An array of bytes
    */
  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asDoubleBuffer.put(array)
    pixels
  }

  /**
    * Return a copy of the present [[DoubleArrayTile]].
    *
    * @return  The copy
    */
  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)

  def withNoData(noDataValue: Option[Double]): Tile =
    DoubleArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: DoubleCells with NoDataHandling =>
        DoubleArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * The [[DoubleRawArrayTile]], derived from [[DoubleArrayTile]].
  */
final case class DoubleRawArrayTile(arr: Array[Double], val cols: Int, val rows: Int)
    extends DoubleArrayTile(arr, cols, rows) {
  val cellType = DoubleCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def apply(i: Int): Int = arr(i).toInt

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def applyDouble(i: Int): Double = arr(i)

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = z.toDouble }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z.toDouble }
}

/**
  * The [[DoubleConstantNoDataArrayTile]], derived from
  * [[DoubleArrayTile]].
  */
final case class DoubleConstantNoDataArrayTile(arr: Array[Double], val cols: Int, val rows: Int)
    extends DoubleArrayTile(arr, cols, rows) {
  val cellType = DoubleConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def apply(i: Int): Int = d2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def applyDouble(i: Int): Double = arr(i)

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2d(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z }
}

/**
  * The [[DoubleUserDefinedNoDataArrayTile]], derived from
  * [[DoubleArrayTile]].
  */
final case class DoubleUserDefinedNoDataArrayTile(arr: Array[Double], val cols: Int, val rows: Int, val cellType: DoubleUserDefinedNoDataCellType)
    extends DoubleArrayTile(arr, cols, rows)
       with UserDefinedDoubleNoDataConversions {
  val userDefinedDoubleNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def apply(i: Int): Int = udd2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The Double datum found at the index
    */
  def applyDouble(i: Int): Double = udd2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2udd(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2udd(z) }
}

/**
  * The companion object for the [[DoubleArrayTile]] type.
  */
object DoubleArrayTile {

  /**
    * Create a new [[DoubleArrayTile]] from an array of doubles, a
    * number of columns, and a number of rows.
    *
    * @param   arr   An array of doubles
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new DoubleArrayTile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int): DoubleArrayTile =
    apply(arr, cols, rows, DoubleConstantNoDataCellType)

  /**
    * Create a new [[DoubleArrayTile]] from an array of doubles, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of doubles
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new DoubleArrayTile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile =
    cellType match {
      case DoubleCellType =>
        new DoubleRawArrayTile(arr, cols, rows)
      case DoubleConstantNoDataCellType =>
        new DoubleConstantNoDataArrayTile(arr, cols, rows)
      case udct: DoubleUserDefinedNoDataCellType =>
        new DoubleUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Create a new [[DoubleArrayTile]] from an array of doubles, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of doubles
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new DoubleArrayTile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int, noDataValue: Option[Double]): DoubleArrayTile =
    apply(arr, cols, rows, DoubleCells.withNoData(noDataValue))

  /**
    * Create a new [[DoubleArrayTile]]  an array of doubles, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of integers
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new DoubleArrayTile
    */
  def apply(arr: Array[Double], cols: Int, rows: Int, noDataValue: Double): DoubleArrayTile =
    apply(arr, cols, rows, Some(noDataValue))


  /**
    * Produce a [[DoubleArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new DoubleArrayTile
    */
  def ofDim(cols: Int, rows: Int): DoubleArrayTile =
    ofDim(cols, rows, DoubleConstantNoDataCellType)

  /**
    * Produce a [[DoubleArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new DoubleArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile =
    cellType match {
      case DoubleCellType =>
        new DoubleRawArrayTile(Array.ofDim[Double](cols * rows), cols, rows)
      case DoubleConstantNoDataCellType =>
        new DoubleConstantNoDataArrayTile(Array.ofDim[Double](cols * rows), cols, rows)
      case udct: DoubleUserDefinedNoDataCellType =>
        new DoubleUserDefinedNoDataArrayTile(Array.ofDim[Double](cols * rows), cols, rows, udct)
    }

  /**
    * Produce an empty, new [[DoubleArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new DoubleArrayTile
    */
  def empty(cols: Int, rows: Int): DoubleArrayTile =
    empty(cols, rows, DoubleConstantNoDataCellType)

  /**
    * Produce an empty, new [[DoubleArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new DoubleArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      ofDim(cols, rows, cellType)
    case DoubleConstantNoDataCellType =>
      fill(doubleNODATA, cols, rows, cellType)
    case DoubleUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  /**
    * Produce a new [[DoubleArrayTile]] and fill it with the given
    * value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new DoubleArrayTile
    */
  def fill(v: Double, cols: Int, rows: Int): DoubleArrayTile =
    fill(v, cols, rows, DoubleConstantNoDataCellType)

  /**
    * Produce a new [[DoubleArrayTile]] and fill it with the given
    * value.  The NODATA value for the tile is inherited from the
    * given cell type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new DoubleArrayTile
    */
  def fill(v: Double, cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      new DoubleRawArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)
    case DoubleConstantNoDataCellType =>
      new DoubleConstantNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows)
    case udct: DoubleUserDefinedNoDataCellType =>
      new DoubleUserDefinedNoDataArrayTile(Array.ofDim[Double](cols * rows).fill(v), cols, rows, udct)
  }

  private def constructDoubleArray(bytes: Array[Byte]): Array[Double] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val doubleBuffer = byteBuffer.asDoubleBuffer()
    val doubleArray = new Array[Double](bytes.size / DoubleConstantNoDataCellType.bytes)
    doubleBuffer.get(doubleArray)
    doubleArray
  }

  /**
    * Produce a new [[DoubleArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new DoubleArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): DoubleArrayTile =
    fromBytes(bytes, cols, rows, DoubleConstantNoDataCellType)

  /**
    * Produce a new [[DoubleArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new DoubleArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: DoubleCells with NoDataHandling): DoubleArrayTile = cellType match {
    case DoubleCellType =>
      new DoubleRawArrayTile(constructDoubleArray(bytes), cols, rows)
    case DoubleConstantNoDataCellType =>
      new DoubleConstantNoDataArrayTile(constructDoubleArray(bytes), cols, rows)
    case udct: DoubleUserDefinedNoDataCellType =>
      new DoubleUserDefinedNoDataArrayTile(constructDoubleArray(bytes), cols, rows, udct)
  }
}
