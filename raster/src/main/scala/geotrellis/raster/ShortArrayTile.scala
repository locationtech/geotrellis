package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
abstract class ShortArrayTile(val array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  def apply(i: Int): Int
  def update(i: Int, z: Int): Unit

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy: ArrayTile = ArrayTile(array.clone, cols, rows)
}

final case class ShortRawArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(arr, cols, rows) {
  val cellType = ShortCellType
  def apply(i: Int): Int = arr(i).toInt
  def update(i: Int, z: Int) { arr(i) = z.toShort }
}

final case class ShortConstantNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends ShortArrayTile(arr, cols, rows) {
  val cellType = ShortConstantNoDataCellType
  def apply(i: Int): Int = s2i(arr(i))
  def update(i: Int, z: Int) = arr(i) = i2s(z)
}

final case class ShortUserDefinedNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int, val cellType: ShortUserDefinedNoDataCellType)
    extends ShortArrayTile(arr, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue
  def apply(i: Int): Int = uds2i(arr(i))
  def update(i: Int, z: Int) { arr(i) = i2uds(z) }
}


object ShortArrayTile {
  def apply(arr: Array[Short], cols: Int, rows: Int): ShortArrayTile =
    apply(arr, cols, rows, ShortConstantNoDataCellType)

  def apply(arr: Array[Short], cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(arr, cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(arr, cols, rows)
      case udct @ ShortUserDefinedNoDataCellType(_) =>
        new ShortUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): ShortArrayTile =
    ofDim(cols, rows, ShortConstantNoDataCellType)

  def ofDim(cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
      case udct @ ShortUserDefinedNoDataCellType(_) =>
        new ShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows, udct)
    }

  def fill(v: Short, cols: Int, rows: Int): ShortArrayTile =
    fill(v, cols, rows, ShortConstantNoDataCellType)

  def fill(v: Short, cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)
      case udct @ ShortUserDefinedNoDataCellType(_) =>
        new ShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows, udct)
    }

  def empty(cols: Int, rows: Int): ShortArrayTile =
    empty(cols, rows, ShortConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)
      case udct @ ShortUserDefinedNoDataCellType(_) =>
        new ShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows, udct)
    }

  private def constructShortArray(bytes: Array[Byte]): Array[Short] = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / ShortCellType.bytes)
    shortBuffer.get(shortArray)
    shortArray
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortArrayTile =
    fromBytes(bytes, cols, rows, ShortConstantNoDataCellType)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: ShortCells with NoDataHandling): ShortArrayTile =
    cellType match {
      case ShortCellType =>
        new ShortRawArrayTile(constructShortArray(bytes), cols, rows)
      case ShortConstantNoDataCellType =>
        new ShortConstantNoDataArrayTile(constructShortArray(bytes), cols, rows)
      case udct @ ShortUserDefinedNoDataCellType(_) =>
        new ShortUserDefinedNoDataArrayTile(constructShortArray(bytes), cols, rows, udct)
    }
}
