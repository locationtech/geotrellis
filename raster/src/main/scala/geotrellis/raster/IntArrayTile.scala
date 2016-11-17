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
  * [[ArrayTile]] based on Array[Int] (each cell as an Int).
  */
abstract class IntArrayTile(val array: Array[Int], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: IntCells with NoDataHandling

  /**
    * Return the array associated with the present [[IntArrayTile]].
    */
  override def toArray = array.clone

  /**
    * Return an array of bytes representing the data behind this
    * [[IntArrayTile]].
    */
  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asIntBuffer.put(array)
    pixels
  }

  /**
    * Return a copy of the present [[IntArrayTile]].
    *
    * @return  The copy
    */
  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)

  def withNoData(noDataValue: Option[Double]): Tile =
    IntArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType): Tile = {
    newCellType match {
      case dt: IntCells with NoDataHandling =>
        IntArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * The [[IntRawArrayTile]] derived from [[IntArrayTile]].
  */
final case class IntRawArrayTile(arr: Array[Int], val cols: Int, val rows: Int)
    extends IntArrayTile(arr, cols, rows) {
  val cellType = IntCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = arr(i)

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = arr(i).toDouble

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = z }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z.toInt }
}

/**
  * The [[IntConstantNoDataArrayTile]] derived from [[IntArrayTile]].
  */
final case class IntConstantNoDataArrayTile(arr: Array[Int], val cols: Int, val rows: Int)
    extends IntArrayTile(arr, cols, rows) {
  val cellType = IntConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = arr(i)

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = i2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = z }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2i(z) }
}

/**
  * The [[IntUserDefinedNoDataArrayTile]] derived from
  * [[IntArrayTile]].
  */
final case class IntUserDefinedNoDataArrayTile(arr: Array[Int], val cols: Int, val rows: Int, val cellType: IntUserDefinedNoDataCellType)
    extends IntArrayTile(arr, cols, rows)
       with UserDefinedIntNoDataConversions {
  val userDefinedIntNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = udi2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = udi2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2udi(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2udi(z) }
}

/**
  * The companion object for the IntArray type.
  */
object IntArrayTile {

  /**
    * Create a new [[IntArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr   An array of integer
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new IntArrayTile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int): IntArrayTile =
    apply(arr, cols, rows, IntConstantNoDataCellType)

  /**
    * Create a new [[IntArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of integers
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new IntArrayTile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(arr, cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(arr, cols, rows)
      case udct: IntUserDefinedNoDataCellType =>
        new IntUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Create a new [[IntArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of integers
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new IntArrayTile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int, noDataValue: Option[Int]): IntArrayTile =
    apply(arr, cols, rows, IntCells.withNoData(noDataValue))

  /**
    * Create a new [[IntArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of integers
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new IntArrayTile
    */
  def apply(arr: Array[Int], cols: Int, rows: Int, noDataValue: Int): IntArrayTile =
   apply(arr, cols, rows, Some(noDataValue))

  /**
    * Produce a [[IntArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new IntArrayTile
    */
  def ofDim(cols: Int, rows: Int): IntArrayTile =
    ofDim(cols, rows, IntConstantNoDataCellType)

  /**
    * Produce a [[IntArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new IntArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(Array.ofDim[Int](cols * rows), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(Array.ofDim[Int](cols * rows), cols, rows)
      case udct: IntUserDefinedNoDataCellType =>
        new IntUserDefinedNoDataArrayTile(Array.ofDim[Int](cols * rows), cols, rows, udct)
    }

  /**
    * Produce an empty, new [[IntArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new IntArrayTile
    */
  def empty(cols: Int, rows: Int): IntArrayTile =
    empty(cols, rows, IntConstantNoDataCellType)

  /**
    * Produce an empty, new [[IntArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new IntArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        ofDim(cols, rows, cellType)
      case IntConstantNoDataCellType =>
        fill(NODATA, cols, rows, cellType)
      case IntUserDefinedNoDataCellType(nd) =>
        fill(nd, cols, rows, cellType)
    }

  /**
    * Produce a new [[IntArrayTile]] and fill it with the given value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new IntArrayTile
    */
  def fill(v: Int, cols: Int, rows: Int): IntArrayTile =
    fill(v, cols, rows, IntConstantNoDataCellType)

  /**
    * Produce a new [[IntArrayTile]] and fill it with the given value.
    * The NODATA value for the tile is inherited from the given cell
    * type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new IntArrayTile
    */
  def fill(v: Int, cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows)
      case udct: IntUserDefinedNoDataCellType =>
        new IntUserDefinedNoDataArrayTile(Array.ofDim[Int](cols * rows).fill(v), cols, rows, udct)
    }

  private def constructIntArray(bytes: Array[Byte]): Array[Int] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](bytes.size / IntConstantNoDataCellType.bytes)
    intBuffer.get(intArray)
    intArray
  }

  /**
    * Produce a new [[IntArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new IntArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): IntArrayTile =
    fromBytes(bytes, cols, rows, IntConstantNoDataCellType)

  /**
    * Produce a new [[IntArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new IntArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: IntCells with NoDataHandling): IntArrayTile =
    cellType match {
      case IntCellType =>
        new IntRawArrayTile(constructIntArray(bytes), cols, rows)
      case IntConstantNoDataCellType =>
        new IntConstantNoDataArrayTile(constructIntArray(bytes), cols, rows)
      case udct: IntUserDefinedNoDataCellType =>
        new IntUserDefinedNoDataArrayTile(constructIntArray(bytes), cols, rows, udct)
    }
}
