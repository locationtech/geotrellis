package geotrellis.raster

import geotrellis.vector.Extent

import java.nio.ByteBuffer
import spire.syntax.cfor._


/**
 * [[ArrayTile]] based on Array[Short] (each cell as a Short).
 */
abstract class ShortArrayTile(val array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile {

  /**
    * Return an array of bytes representing the data behind this
    * [[ShortArrayTile]].
    */
  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  /**
    * Return a copy of the present [[ShortArrayTile]].
    *
    * @return  The copy
    */
  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)
}

/**
  * [[ShortRawArrayTile]] derived from [[ShortArrayTile]].
  */
final case class ShortRawArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(arr, cols, rows) {
  val cellType = ShortCellType

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
  * [[ShortConstantNoDataArrayTile]] derived from [[ShortArrayTile]].
  */
final case class ShortConstantNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(arr, cols, rows) {
  val cellType = ShortConstantNoDataCellType

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = s2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = s2d(arr(i))

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def update(i: Int, z: Int) = arr(i) = i2s(z)

  /**
    * Update the datum at the given index in the array.
    *
    * @param   i  The index
    * @param   z  The value to place at that index
    */
  def updateDouble(i: Int, z: Double) = arr(i) = d2s(z)
}

/**
  * [[ShortUserDefinedNoDataArrayTile]] derived from
  * [[ShortArrayTile]].
  */
final case class ShortUserDefinedNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int, val cellType: ShortUserDefinedNoDataCellType)
    extends ShortArrayTile(arr, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def apply(i: Int): Int = uds2i(arr(i))

  /**
    * Fetch the datum at the given index in the array.
    *
    * @param   i  The index
    * @return     The datum found at the index
    */
  def applyDouble(i: Int): Double = uds2d(arr(i))

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
  * The companion object associated with the [[ShortArrayTile]] type.
  */
object ShortArrayTile {

  /**
    * Create a new [[ShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr   An array of integer
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        A new ShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int): ShortArrayTile =
    apply(arr, cols, rows, ShortConstantNoDataCellType)

  /**
    * Create a new [[ShortArrayTile]] from an array of integers, a
    * number of columns, and a number of rows.
    *
    * @param   arr       An array of integers
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The type from which to take the NODATA value
    * @return            A new ShortArrayTile
    */
  def apply(arr: Array[Short], cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(arr, cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(arr, cols, rows)
      case udct: ShortUserDefinedNoDataCellType =>
        new ShortUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  /**
    * Produce a [[ShortArrayTile]] of the specified dimensions.
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ShortArrayTile
    */
  def ofDim(cols: Int, rows: Int): ShortArrayTile =
    ofDim(cols, rows, ShortConstantNoDataCellType)

  /**
    * Produce a [[ShortArrayTile]] of the specified dimensions.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ShortArrayTile
    */
  def ofDim(cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
      case udct: ShortUserDefinedNoDataCellType =>
        new ShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows, udct)
    }

  /**
    * Produce an empty, new [[ShortArrayTile]].
    *
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ShortArrayTile
    */
  def empty(cols: Int, rows: Int): ShortArrayTile =
    empty(cols, rows, ShortConstantNoDataCellType)

  /**
    * Produce an empty, new [[ShortArrayTile]].  The NODATA type for
    * the tile is derived from the given cell type.
    *
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ShortArrayTile
    */
  def empty(cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        ofDim(cols, rows, cellType)
      case ShortConstantNoDataCellType =>
        fill(shortNODATA, cols, rows, cellType)
      case ShortUserDefinedNoDataCellType(nd) =>
        fill(nd, cols, rows, cellType)
    }

  /**
    * Produce a new [[ShortArrayTile]] and fill it with the given value.
    *
    * @param   v     the values to fill into the new tile
    * @param   cols  The number of columns
    * @param   rows  The number of rows
    * @return        The new ShortArrayTile
    */
  def fill(v: Short, cols: Int, rows: Int): ShortArrayTile =
    fill(v, cols, rows, ShortConstantNoDataCellType)

  /**
    * Produce a new [[ShortArrayTile]] and fill it with the given value.
    * The NODATA value for the tile is inherited from the given cell
    * type.
    *
    * @param   v         the values to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ShortArrayTile
    */
  def fill(v: Short, cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
      case udct: ShortUserDefinedNoDataCellType =>
        new ShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows, udct)
    }

  private def constructShortArray(bytes: Array[Byte]): Array[Short] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortCellType.bytes)
    shortBuffer.get(shortArray)
    shortArray
  }

  /**
    * Produce a new [[ShortArrayTile]] from an array of bytes.
    *
    * @param   bytes  the data to fill into the new tile
    * @param   cols   The number of columns
    * @param   rows   The number of rows
    * @return         The new ShortArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortArrayTile =
    fromBytes(bytes, cols, rows, ShortConstantNoDataCellType)

  /**
    * Produce a new [[ShortArrayTile]] from an array of bytes.  The
    * NODATA value for the tile is inherited from the given cell type.
    *
    * @param   bytes     the data to fill into the new tile
    * @param   cols      The number of columns
    * @param   rows      The number of rows
    * @param   cellType  The cell type from which to derive the NODATA
    * @return            The new ShortArrayTile
    */
  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(constructShortArray(bytes), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(constructShortArray(bytes), cols, rows)
      case udct: ShortUserDefinedNoDataCellType =>
        new ShortUserDefinedNoDataArrayTile(constructShortArray(bytes), cols, rows, udct)
    }
}
