package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
abstract class UShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {
  val cellType: UShortCells with NoDataHandling

  def apply(i: Int): Int
  def update(i: Int, z: Int)

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = UShortArrayTile(array.clone, cols, rows)
}

class UShortRawArrayTile(array: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(array, cols, rows) {
  val cellType = UShortCellType
  def apply(i: Int): Int = array(i) & 0xFF
  def update(i: Int, z: Int) { array(i) = (z & 0xFF).toShort }
}

class UShortConstantNoDataCellTypeArrayTile(array: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(array, cols, rows) {
  val cellType = UShortConstantNoDataCellType

  def apply(i: Int): Int = us2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2us(z) }
}

class UShortUserDefinedNoDataArrayTile(array: Array[Short], val cols: Int, val rows: Int, val cellType: UShortUserDefinedNoDataCellType)
    extends UShortArrayTile(array, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue.toShort

  def apply(i: Int): Int = uds2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2uds(z) }
}

object UShortArrayTile {
  def apply(arr: Array[Short], cols: Int, rows: Int) =
    new UShortConstantNoDataCellTypeArrayTile(arr, cols, rows)

  def ofDim(cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataCellTypeArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataCellTypeArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def fill(v: Short, cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataCellTypeArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  private def constructShortArray(bytes: Array[Byte]): Array[Short] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortCellType.bytes)
    shortBuffer.get(shortArray)
    shortArray
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataCellTypeArrayTile(constructShortArray(bytes), cols, rows)

  def fromBytesRaw(bytes: Array[Byte], cols: Int, rows: Int): UShortArrayTile =
    new UShortRawArrayTile(constructShortArray(bytes), cols, rows)

  def fromBytesUserDefined(bytes: Array[Byte], cols: Int, rows: Int, noDataValue: Short): UShortArrayTile =
    new UShortUserDefinedNoDataArrayTile(constructShortArray(bytes), cols, rows, UShortUserDefinedNoDataCellType(noDataValue))

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): UShortArrayTile =
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
      val shortBuffer = byteBuffer.asShortBuffer()
      val len = bytes.length / UShortConstantNoDataCellType.bytes
      val shortArray = new Array[Short](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val v = shortBuffer.get(i)
        if(v == replaceNoData)
          shortArray(i) = shortNODATA
        else
          shortArray(i) = v
      }

      new UShortConstantNoDataCellTypeArrayTile(shortArray, cols, rows)
    }
}
