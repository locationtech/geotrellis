package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
abstract class UShortArrayTile(val array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: UShortCells with NoDataHandling

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = UShortArrayTile(array.clone, cols, rows)
}

class UShortRawArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(arr, cols, rows) {
  val cellType = UShortCellType
  def apply(i: Int): Int = arr(i) & 0xFFFF
  def applyDouble(i: Int): Double = (arr(i) & 0xFFFF).toDouble
  def update(i: Int, z: Int) { arr(i) = z.toShort }
  def updateDouble(i: Int, z: Double) { arr(i) = z.toShort }
}

class UShortConstantNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int)
    extends UShortArrayTile(arr, cols, rows) {
  val cellType = UShortConstantNoDataCellType

  def apply(i: Int): Int = us2i(arr(i))
  def applyDouble(i: Int): Double = us2d(arr(i))
  def update(i: Int, z: Int) { arr(i) = i2us(z) }
  def updateDouble(i: Int, z: Double) { arr(i) = d2us(z) }
}

class UShortUserDefinedNoDataArrayTile(arr: Array[Short], val cols: Int, val rows: Int, val cellType: UShortUserDefinedNoDataCellType)
    extends UShortArrayTile(arr, cols, rows)
       with UserDefinedShortNoDataConversions {
  val userDefinedShortNoDataValue = cellType.noDataValue

  def apply(i: Int): Int = udus2i(arr(i))
  def applyDouble(i: Int): Double = udus2d(arr(i))
  def update(i: Int, z: Int) { arr(i) = i2uds(z) }
  def updateDouble(i: Int, z: Double) { arr(i) = d2uds(z) }
}

object UShortArrayTile {
  def apply(arr: Array[Short], cols: Int, rows: Int): UShortArrayTile =
    apply(arr, cols, rows, UShortConstantNoDataCellType)

  def apply(arr: Array[Short], cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      new UShortRawArrayTile(arr, cols, rows)
    case UShortConstantNoDataCellType =>
      new UShortConstantNoDataArrayTile(arr, cols, rows)
    case udct: UShortUserDefinedNoDataCellType =>
      new UShortUserDefinedNoDataArrayTile(arr, cols, rows, udct)
  }

  def ofDim(cols: Int, rows: Int): UShortArrayTile =
    new UShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def ofDim(cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      new UShortRawArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
    case UShortConstantNoDataCellType =>
      new UShortConstantNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows)
    case udct: UShortUserDefinedNoDataCellType =>
      new UShortUserDefinedNoDataArrayTile(Array.ofDim[Short](cols * rows), cols, rows, udct)
  }

  def empty(cols: Int, rows: Int): UShortArrayTile =
    empty(cols, rows, UShortConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: UShortCells with NoDataHandling): UShortArrayTile = cellType match {
    case UShortCellType =>
      ofDim(cols, rows, cellType)
    case UShortConstantNoDataCellType =>
      fill(ushortNODATA, cols, rows, cellType)
    case UShortUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  def fill(v: Short, cols: Int, rows: Int): UShortArrayTile =
    fill(v, cols, rows, UShortConstantNoDataCellType)

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

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UShortArrayTile =
    fromBytes(bytes, cols, rows, UShortConstantNoDataCellType)

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
