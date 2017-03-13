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


/**
 * [[ArrayTile]] based on Array[Byte] (each cell as a Byte).
 */
abstract class ByteArrayTile(val array: Array[Byte], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: ByteCells with NoDataHandling

  /**
    * Convert the present [[ByteArrayTile]] to an array of bytes and
    * return that array.
    *
    * @return  An array of bytes
    */
  def toBytes: Array[Byte] = array.clone

  /**
    * Return a copy of the present [[ByteArrayTile]].
    *
    * @return  The copy
    */
  def copy: ByteArrayTile = ArrayTile(array.clone, cols, rows)

  def withNoData(noDataValue: Option[Double]): Tile =
    ByteArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

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
  * A [[ByteRawArrayTile]], created from the given array, with the
  * given number of columns and rows.
  *
  * @param  arr   The array of bytes from which to initialize the tile
  * @param  cols  The number of columns
  * @param  rows  The number of rows
  */
final case class ByteRawArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(arr, cols, rows) {
  val cellType = ByteCellType

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def apply(i: Int): Int = arr(i).toInt

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def applyDouble(i: Int): Double = arr(i).toDouble

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def update(i: Int, z: Int) { arr(i) = z.toByte }

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z.toByte }
}

/**
  * A [[ByteConstantNoDataArrayTile]], created from the given array,
  * with the given number of columns and rows.
  *
  * @param  arr   The array of bytes from which to initialize the tile
  * @param  cols  The number of columns
  * @param  rows  The number of rows
  */
final case class ByteConstantNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(arr, cols, rows) {
  val cellType = ByteConstantNoDataCellType

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def apply(i: Int): Int = b2i(arr(i))

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def applyDouble(i: Int): Double = b2d(arr(i))

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def update(i: Int, z: Int) { arr(i) = i2b(z) }

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2b(z) }
}

/**
  * A [[ByteUserDefinedNoDataArrayTile]], created from the given
  * array, with the given number of columns and rows.
  *
  * @param  arr       The array of bytes from which to initialize the tile
  * @param  cols      The number of columns
  * @param  rows      The number of rows
  * @param  cellType  The cellType whose NODATA value is to be used
  */
final case class ByteUserDefinedNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int, val cellType: ByteUserDefinedNoDataCellType)
    extends ByteArrayTile(arr, cols, rows)
       with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = cellType.noDataValue

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def apply(i: Int): Int = { udb2i(arr(i)) }

  /**
    * Get the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @return     The datum
    */
  def applyDouble(i: Int): Double = { udb2d(arr(i)) }

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def update(i: Int, z: Int) { arr(i) = i2udb(z) }

  /**
    * Update the datum at the specified index.
    *
    * @param   i  The index of the datum
    * @param   z  The datum
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2udb(z) }
}

/**
  * The companion object for the [[ByteArrayTile]] type.
  */
object ByteArrayTile {

  /**
    * Create a new [[ByteArrayTile]] from a given array of bytes with
    * the given number of columns and rows.
    *
    * @param   arr   An array of bytes
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    apply(arr, cols, rows, ByteConstantNoDataCellType)

  /**
    * Create a new [[ByteArrayTile]] from a given array of bytes with
    * the given number of columns and rows, with the NODATA value
    * inherited from the specified cell type.
    *
    * @param   arr       An array of bytes
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =
    cellType match {
      case ByteCellType =>
        new ByteRawArrayTile(arr, cols, rows)
      case ByteConstantNoDataCellType =>
        new ByteConstantNoDataArrayTile(arr, cols, rows)
      case udct: ByteUserDefinedNoDataCellType =>
        new ByteUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Create a new [[ByteArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of bytes
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new ByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, noDataValue: Option[Byte]): ByteArrayTile =
    apply(arr, cols, rows, ByteCells.withNoData(noDataValue))

  /**
    * Create a new [[ByteArrayTile]] from an array of bytes, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of bytes
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new ByteArrayTile
    */
  def apply(arr: Array[Byte], cols: Int, rows: Int, noDataValue: Byte): ByteArrayTile =
    apply(arr, cols, rows, Some(noDataValue))

  /**
    * Produce a [[ByteArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ByteArrayTile
    */
  def ofDim(cols: Int, rows: Int): ByteArrayTile =
    ofDim(cols, rows, ByteConstantNoDataCellType)

  /**
    * Produce a [[ByteArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ByteArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =  cellType match {
    case ByteCellType =>
      new ByteRawArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case ByteConstantNoDataCellType =>
      new ByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case udct: ByteUserDefinedNoDataCellType =>
      new ByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows, udct)
  }

  /**
    * Produce an empty, new [[ByteArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ByteArrayTile
    */
  def empty(cols: Int, rows: Int): ByteArrayTile =
    empty(cols, rows, ByteConstantNoDataCellType)

  /**
    * Produce an empty, new [[ByteArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ByteArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile = cellType match {
    case ByteCellType =>
      ofDim(cols, rows, cellType)
    case ByteConstantNoDataCellType =>
      fill(byteNODATA, cols, rows, cellType)
    case ByteUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  /**
    * Produce a new [[ByteArrayTile]] and fill it with the given value.
    *
    * @param   v     The value to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ByteArrayTile
    */
  def fill(v: Byte, cols: Int, rows: Int): ByteArrayTile =
    fill(v, cols, rows, ByteConstantNoDataCellType)

  /**
    * Produce a new [[ByteArrayTile]] and fill it with the given value.
    * The NODATA value for the tile is inherited from the given cell
    * type.
    *
    * @param   v         The value to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ByteArrayTile
    */
  def fill(v: Byte, cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile = cellType match {
    case ByteCellType =>
      new ByteRawArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case ByteConstantNoDataCellType =>
      new ByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case udct: ByteUserDefinedNoDataCellType =>
      new ByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows, udct)
  }

  /**
    * Produce a new [[ByteArrayTile]] from an array of bytes.
    *
    * @param   bytes  the values to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new ByteArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    fromBytes(bytes, cols, rows, ByteConstantNoDataCellType)

  /**
    * Produce a new [[ByteArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ByteArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =
    cellType match {
      case ByteCellType =>
        new ByteRawArrayTile(bytes.clone, cols, rows)
      case ByteConstantNoDataCellType =>
        new ByteConstantNoDataArrayTile(bytes.clone, cols, rows)
      case udct: ByteUserDefinedNoDataCellType =>
        new ByteUserDefinedNoDataArrayTile(bytes.clone, cols, rows, udct)
    }
}
