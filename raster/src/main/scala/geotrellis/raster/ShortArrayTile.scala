package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
abstract class ShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  def apply(i: Int): Int
  def update(i: Int, z: Int): Unit

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

class ShortRawArrayTile(array: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(array, cols, rows) {
  val cellType = ShortCellType
  def apply(i: Int): Int = array(i).toInt
  def update(i: Int, z: Int) { array(i) = z.toShort }
}

class ShortConstantNoDataArrayTile(array: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(array, cols, rows) {
  val cellType = ShortConstantNoDataCellType

  def apply(i: Int): Int = s2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2s(z) }
}

class ShortUserDefinedNoDataArrayTile(array: Array[Short], val cols: Int, val rows: Int, val cellType: ShortUserDefinedNoDataCellType)
    extends ShortArrayTile(array, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue

  def apply(i: Int): Int = uds2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2uds(z) }
}


object ShortArrayTile {
  def apply(arr: Array[Short], cols: Int, rows: Int) =
    new ShortConstantNoDataArrayTile(arr, cols, rows)

  def apply(arr: Array[Short], cols: Int, rows: Int, cellType: ShortUserDefinedNoDataCellType) =
    new ShortUserDefinedNoDataArrayTile(arr, cols, rows, cellType)

  def fill(v: Short, cols: Int, rows: Int): ShortArrayTile =
    new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def ofDim(cols: Int, rows: Int): ShortArrayTile =
    new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ShortArrayTile =
    new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)

  def fromRawBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortRawArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortCellType.bytes)
    shortBuffer.get(shortArray)

    new ShortRawArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortConstantNoDataCellType.bytes)
    shortBuffer.get(shortArray)

    new ShortConstantNoDataArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): ShortArrayTile =
    if (isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
      val shortBuffer = byteBuffer.asShortBuffer()
      val len = bytes.length / ShortConstantNoDataCellType.bytes
      val shortArray = new Array[Short](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val v = shortBuffer.get(i)
        if(v == replaceNoData)
          shortArray(i) = shortNODATA
        else
          shortArray(i) = v
      }
      new ShortConstantNoDataArrayTile(shortArray, cols, rows)
    }
}
