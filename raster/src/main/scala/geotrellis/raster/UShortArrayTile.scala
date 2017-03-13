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
  * [[ArrayTile]] based on Array[Short] (each cell as a Short).
  */
abstract class UShortArrayTile(val array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: UShortCells with NoDataHandling

  /**
    * Return an array of bytes representing the data behind this
    * [[UShortArrayTile]].
    */
  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  /**
    * Return a copy of the present [[UShortArrayTile]].
    *
    * @return  The copy
    */
  def copy = UShortArrayTile(array.clone, cols, rows)

  def withNoData(noDataValue: Option[Double]): Tile =
    UShortArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: ShortCells with NoDataHandling =>
        ShortArrayTile(array, cols, rows, dt)
      case dt: UShortCells with NoDataHandling =>
        UShortArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * [[UShortRawArrayTile]] derived from [[UShortArrayTile]].
  */
class UShortRawArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(arr, cols, rows) {
  val cellType = UShortCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = arr(i) & 0xFFFF

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = (arr(i) & 0xFFFF).toDouble

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = z.toShort }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z.toShort }
}

/**
  * [[UShortConstantNoDataArrayTile]] derived from [[UShortArrayTile]].
  */
class UShortConstantNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(arr, cols, rows) {
  val cellType = UShortConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = us2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = us2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2us(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2us(z) }
}

/**
  * [[UShortUserDefinedNoDataArrayTile]] derived from
  * [[UShortArrayTile]].
  */
class UShortUserDefinedNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int, val cellType: UShortUserDefinedNoDataCellType)
    extends UShortArrayTile(arr, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = udus2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = udus2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2uds(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2uds(z) }
}

/**
  * The companion object associated with the [[UShortArrayTile]] type.
  */
object UShortArrayTile {

  /**
    * Create a new [[UShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr   An array of integer
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new UShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int): UShortArrayTile =
    apply(arr, cols, rows, UShortConstantNoDataCellType)

  /**
    * Create a new [[UShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of integers
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new UShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      new UShortRawArrayTile(arr, cols, rows)
    case UShortConstantNoDataCellType =>
      new UShortConstantNoDataArrayTile(arr, cols, rows)
    case udct: UShortUserDefinedNoDataCellType =>
      new UShortUserDefinedNoDataArrayTile(arr, cols, rows, udct)
  }

  /**
    * Create a new [[UShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of integers
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new UShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int, noDataValue: Option[Short]): UShortArrayTile =
    apply(arr, cols, rows, UShortCells.withNoData(noDataValue))

  /**
    * Create a new [[UShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of integers
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new UShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int, noDataValue: Short): UShortArrayTile =
    apply(arr, cols, rows, Some(noDataValue))

  /**
    * Produce a [[UShortArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UShortArrayTile
    */
  def ofDim(cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  /**
    * Produce a [[UShortArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UShortArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      new UShortRawArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
    case UShortConstantNoDataCellType =>
      new UShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
    case udct: UShortUserDefinedNoDataCellType =>
      new UShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows, udct)
  }

  /**
    * Produce an empty [[UShortArrayTile]] of the given dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UShortArrayTile
    */
  def empty(cols: Int, rows: Int): UShortArrayTile =
    empty(cols, rows, UShortConstantNoDataCellType)

  /**
    * Produce an empty [[UShortArrayTile]] of the given dimensions.
    * The NODATA type for the tile is derived from the given cell
    * type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UShortArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      ofDim(cols, rows, cellType)
    case UShortConstantNoDataCellType =>
      fill(ushortNODATA, cols, rows, cellType)
    case UShortUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  /**
    * Produce a new [[UShortArrayTile]] and fill it with the given
    * value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UShortArrayTile
    */
  def fill(v: Short, cols: Int, rows: Int): UShortArrayTile =
    fill(v, cols, rows, UShortConstantNoDataCellType)

  /**
    * Produce a new [[UShortArrayTile]] and fill it with the given
    * value.  The NODATA value for the tile is inherited from the
    * given cell type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UShortArrayTile
    */
  def fill(v: Short, cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      new UShortRawArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
    case UShortConstantNoDataCellType =>
      new UShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
    case udct: UShortUserDefinedNoDataCellType =>
      new UShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows, udct)
  }

  private def constructShortArray(bytes: Array[Byte]): Array[Short] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortCellType.bytes)
    shortBuffer.get(shortArray)
    shortArray
  }

  /**
    * Produce a new [[UShortArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new UShortArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UShortArrayTile =
    fromBytes(bytes, cols, rows, UShortConstantNoDataCellType)

  /**
    * Produce a new [[UShortArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UShortArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile =
    cellType match {
      case UShortCellType =>
        new UShortRawArrayTile(constructShortArray(bytes.clone), cols, rows)
      case UShortConstantNoDataCellType =>
        new UShortConstantNoDataArrayTile(constructShortArray(bytes.clone), cols, rows)
      case udct: UShortUserDefinedNoDataCellType =>
        new UShortUserDefinedNoDataArrayTile(constructShortArray(bytes.clone), cols, rows, udct)
    }
}
