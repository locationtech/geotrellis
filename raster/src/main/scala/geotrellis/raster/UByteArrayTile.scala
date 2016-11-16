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

import geotrellis.vector.Extent

import java.nio.ByteBuffer
import spire.syntax.cfor._


/**
  * [[ArrayTile]] based on Array[Byte] (each cell as a Byte).
  */
abstract class UByteArrayTile(val array: Array[Byte], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: UByteCells with NoDataHandling

  /**
    * Return an array of bytes representing the data behind this
    * [[UByteArrayTile]].
    */
  def toBytes: Array[Byte] = array.clone

  /**
    * Return a copy of the present [[UByteArrayTile]].
    *
    * @return  The copy
    */
  def copy = UByteArrayTile(array.clone, cols, rows, cellType)

  def withNoData(noDataValue: Option[Double]): Tile =
    UByteArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: ByteCells with NoDataHandling =>
        ByteArrayTile(array, cols, rows, dt)
      case dt: UByteCells with NoDataHandling =>
        UByteArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * [[UByteRawArrayTile]] derived from [[UByteArrayTile]].
  */
final case class UByteRawArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends UByteArrayTile(arr, cols, rows) {
  val cellType = UByteCellType
  def apply(i: Int): Int = arr(i) & 0xFF
  def applyDouble(i: Int): Double = (arr(i) & 0xFF).toDouble
  def update(i: Int, z: Int) { arr(i) = z.toByte }
  def updateDouble(i: Int, z: Double) { arr(i) = z.toByte }
}

/**
  * [[UByteConstantNoDataArrayTile]] derived from [[UByteArrayTile]].
  */
final case class UByteConstantNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends UByteArrayTile(arr, cols, rows) {
  val cellType = UByteConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = ub2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = ub2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2ub(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2ub(z) }
}

/**
  * [[UByteUserDefinedNoDataArrayTile]] derived from
  * [[UByteArrayTile]].
  */
final case class UByteUserDefinedNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int, val cellType: UByteUserDefinedNoDataCellType)
    extends UByteArrayTile(arr, cols, rows)
       with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = udub2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = udub2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2udb(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2udb(z) }
}

/**
  * The companion object for the [[UByteArrayTile]] type.
  */
object UByteArrayTile {

  /**
    * Create a new [[UByteArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr   An array of integer
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new UByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int): UByteArrayTile =
    apply(arr, cols, rows, UByteConstantNoDataCellType)

  /**
    * Create a new [[UByteArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of integers
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new UByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, cellType: UByteCells with NoDataHandling): UByteArrayTile =
    cellType match {
      case UByteCellType =>
        new UByteRawArrayTile(arr, cols, rows)
      case UByteConstantNoDataCellType =>
        new UByteConstantNoDataArrayTile(arr, cols, rows)
      case udct: UByteUserDefinedNoDataCellType =>
        new UByteUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Create a new [[UByteArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of bytes
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new UByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, noDataValue: Option[Byte]): UByteArrayTile =
    apply(arr, cols, rows, UByteCells.withNoData(noDataValue))

  /**
    * Create a new [[UByteArrayTile]] from an array of bytes, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of bytes
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new UByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, noDataValue: Byte): UByteArrayTile =
    apply(arr, cols, rows, Some(noDataValue))


  /**
    * Produce a [[UByteArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UByteArrayTile
    */
  def ofDim(cols: Int, rows: Int): UByteArrayTile =
    ofDim(cols, rows, UByteConstantNoDataCellType)

  /**
    * Produce a [[UByteArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UByteArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: UByteCells with NoDataHandling): UByteArrayTile =  cellType match {
    case UByteCellType =>
      new UByteRawArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case UByteConstantNoDataCellType =>
      new UByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case udct: UByteUserDefinedNoDataCellType =>
      new UByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows, udct)
  }

  /**
    * Produce an empty, new [[UByteArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UByteArrayTile
    */
  def empty(cols: Int, rows: Int): UByteArrayTile =
    empty(cols, rows, UByteConstantNoDataCellType)

  /**
    * Produce an empty, new [[UByteArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UByteArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: UByteCells with NoDataHandling): UByteArrayTile = cellType match {
    case UByteCellType =>
      ofDim(cols, rows, cellType)
    case UByteConstantNoDataCellType =>
      fill(ubyteNODATA, cols, rows, cellType)
    case UByteUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  /**
    * Produce a new [[UByteArrayTile]] and fill it with the given value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new UByteArrayTile
    */
  def fill(v: Byte, cols: Int, rows: Int): UByteArrayTile =
    fill(v, cols, rows, UByteConstantNoDataCellType)

  /**
    * Produce a new [[UByteArrayTile]] and fill it with the given value.
    * The NODATA value for the tile is inherited from the given cell
    * type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UByteArrayTile
    */
  def fill(v: Byte, cols: Int, rows: Int, cellType: UByteCells with NoDataHandling): UByteArrayTile = cellType match {
    case UByteCellType =>
      new UByteRawArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case UByteConstantNoDataCellType =>
      new UByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case udct: UByteUserDefinedNoDataCellType =>
      new UByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows, udct)
  }

  /**
    * Produce a new [[UByteArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new UByteArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UByteArrayTile =
    fromBytes(bytes, cols, rows, UByteConstantNoDataCellType)

  /**
    * Produce a new [[UByteArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new UByteArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: UByteCells with NoDataHandling): UByteArrayTile =
    cellType match {
      case UByteCellType =>
        new UByteRawArrayTile(bytes.clone, cols, rows)
      case UByteConstantNoDataCellType =>
        new UByteConstantNoDataArrayTile(bytes.clone, cols, rows)
      case udct: UByteUserDefinedNoDataCellType =>
        new UByteUserDefinedNoDataArrayTile(bytes.clone, cols, rows, udct)
    }
}
