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
  * [[ArrayTile]] based on Array[Float] (each cell as a Float).
  */
abstract class FloatArrayTile(val array: Array[Float], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: FloatCells with NoDataHandling

  /**
    * Convert the present [[FloatArrayTile]] to an array of bytes and
    * return that array.
    *
    * @return  An array of bytes
    */
  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.size * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asFloatBuffer.put(array)
    pixels
  }

  /**
    * Return a copy of the present [[FloatArrayTile]].
    *
    * @return  The copy
    */
  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)

  def withNoData(noDataValue: Option[Double]): FloatArrayTile =
    FloatArrayTile(array, cols, rows, cellType.withNoData(noDataValue))

  def interpretAs(newCellType: CellType) = {
    newCellType match {
      case dt: FloatCells with NoDataHandling =>
        FloatArrayTile(array, cols, rows, dt)
      case _ =>
        withNoData(None).convert(newCellType)
    }
  }
}

/**
  * The [[FloatRawArrayTile]] derived from [[FloatArrayTile]].
  */
final case class FloatRawArrayTile(arr: Array[Float], val cols: Int, val rows: Int)
    extends FloatArrayTile(arr, cols, rows) {
  val cellType = FloatCellType
  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = arr(i).toInt

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
  def update(i: Int, z: Int) { arr(i) = z.toFloat }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = z.toFloat }
}

/**
  * The [[FloatConstantNoDataArrayTile]] derived from
  * [[FloatArrayTile]].
  */
final case class FloatConstantNoDataArrayTile(arr: Array[Float], val cols: Int, val rows: Int)
    extends FloatArrayTile(arr, cols, rows) {
  val cellType = FloatConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = f2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = f2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2f(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2f(z) }
}

/**
  * The [[FloatUserDefinedNoDataArrayTile]] derived from
  * [[FloatArrayTile]].
  */
final case class FloatUserDefinedNoDataArrayTile(arr: Array[Float], val cols: Int, val rows: Int, val cellType: FloatUserDefinedNoDataCellType)
    extends FloatArrayTile(arr, cols, rows)
       with UserDefinedFloatNoDataConversions {
  val userDefinedFloatNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = udf2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = udf2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) { arr(i) = i2udf(z) }

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) { arr(i) = d2udf(z) }
}

/**
  * The companion object for the [[FloatArrayTile]] type.
  */
object FloatArrayTile {

  /**
    * Create a new [[FloatArrayTile]] from an array of floats, a number
    * of columns, and a number of rows.
    *
    * @param   arr   An array of float
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new FloatArrayTile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int): FloatArrayTile =
    apply(arr, cols, rows, FloatConstantNoDataCellType)

  /**
    * Create a new [[FloatArrayTile]] from an array of floats, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of floats
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new FloatArrayTile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(arr, cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(arr, cols, rows)
      case udct: FloatUserDefinedNoDataCellType =>
        new FloatUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Create a new [[DoubleArrayTile]] from an array of floats, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of floats
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  Optional NODATA value
    * @return               A new DoubleArrayTile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int, noDataValue: Option[Float]): FloatArrayTile =
    apply(arr, cols, rows, FloatCells.withNoData(noDataValue))

  /**
    * Create a new [[DoubleArrayTile]]  an array of floats, a
    * number of columns, and a number of rows.
    *
    * @param   arr          An array of floats
    * @param   cols         The number of columns
    * @param   rows         The number of rows
    * @param   noDataValue  NODATA value
    * @return               A new DoubleArrayTile
    */
  def apply(arr: Array[Float], cols: Int, rows: Int, noDataValue: Float): FloatArrayTile =
    apply(arr, cols, rows, Some(noDataValue))

  /**
    * Produce a [[FloatArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new FloatArrayTile
    */
  def ofDim(cols: Int, rows: Int): FloatArrayTile =
    ofDim(cols, rows, FloatConstantNoDataCellType)

  /**
    * Produce a [[FloatArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new FloatArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(Array.ofDim[Float](cols * rows), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(Array.ofDim[Float](cols * rows), cols, rows)
      case udct: FloatUserDefinedNoDataCellType =>
        new FloatUserDefinedNoDataArrayTile(Array.ofDim[Float](cols * rows), cols, rows, udct)
    }

  /**
    * Produce an empty, new [[FloatArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new FloatArrayTile
    */
  def empty(cols: Int, rows: Int): FloatArrayTile =
    empty(cols, rows, FloatConstantNoDataCellType)

  /**
    * Produce an empty, new [[FloatArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new FloatArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        ofDim(cols, rows, cellType)
      case FloatConstantNoDataCellType =>
        fill(Float.NaN, cols, rows, cellType)
      case FloatUserDefinedNoDataCellType(nd) =>
        fill(nd, cols, rows, cellType)
    }

  /**
    * Produce a new [[FloatArrayTile]] and fill it with the given
    * value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new FloatArrayTile
    */
  def fill(v: Float, cols: Int, rows: Int): FloatArrayTile =
    fill(v, cols, rows, FloatConstantNoDataCellType)

  /**
    * Produce a new [[FloatArrayTile]] and fill it with the given
    * value.  The NODATA value for the tile is inherited from the
    * given cell type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new FloatArrayTile
    */
  def fill(v: Float, cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows)
      case udct: FloatUserDefinedNoDataCellType =>
        new FloatUserDefinedNoDataArrayTile(Array.ofDim[Float](cols * rows).fill(v), cols, rows, udct)
    }

  private def constructFloatArray(bytes: Array[Byte]): Array[Float] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.size)
    val floatBuffer = byteBuffer.asFloatBuffer()
    val floatArray = new Array[Float](bytes.size / FloatConstantNoDataCellType.bytes)
    floatBuffer.get(floatArray)
    floatArray
  }

  /**
    * Produce a new [[FloatArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new FloatArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): FloatArrayTile =
    fromBytes(bytes, cols, rows, FloatConstantNoDataCellType)

  /**
    * Produce a new [[FloatArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new FloatArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: FloatCells with NoDataHandling): FloatArrayTile =
    cellType match {
      case FloatCellType =>
        new FloatRawArrayTile(constructFloatArray(bytes), cols, rows)
      case FloatConstantNoDataCellType =>
        new FloatConstantNoDataArrayTile(constructFloatArray(bytes), cols, rows)
      case udct: FloatUserDefinedNoDataCellType =>
        new FloatUserDefinedNoDataArrayTile(constructFloatArray(bytes), cols, rows, udct)
    }
}
