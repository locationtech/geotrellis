package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.resample._

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
abstract class ByteArrayTile(val array: Array[Byte], cols: Int, rows: Int)
    extends MutableArrayTile {
  val cellType: ByteCells with NoDataHandling

  def toBytes: Array[Byte] = array.clone
  def copy: ByteArrayTile = ArrayTile(array.clone, cols, rows)
}

final case class ByteRawArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(arr, cols, rows) {
  val cellType = ByteCellType
  def apply(i: Int): Int = arr(i).toInt
  def applyDouble(i: Int): Double = arr(i).toDouble
  def update(i: Int, z: Int) { arr(i) = z.toByte }
  def updateDouble(i: Int, z: Double) { arr(i) = z.toByte }
}

final case class ByteConstantNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(arr, cols, rows) {
  val cellType = ByteConstantNoDataCellType
  def apply(i: Int): Int = b2i(arr(i))
  def applyDouble(i: Int): Double = b2d(arr(i))
  def update(i: Int, z: Int) { arr(i) = i2b(z) }
  def updateDouble(i: Int, z: Double) { arr(i) = d2b(z) }
}

final case class ByteUserDefinedNoDataArrayTile(arr: Array[Byte], val cols: Int, val rows: Int, val cellType: ByteUserDefinedNoDataCellType)
    extends ByteArrayTile(arr, cols, rows)
       with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = cellType.noDataValue
  def apply(i: Int): Int = { udb2i(arr(i)) }
  def applyDouble(i: Int): Double = { udb2d(arr(i)) }
  def update(i: Int, z: Int) { arr(i) = i2udb(z) }
  def updateDouble(i: Int, z: Double) { arr(i) = d2udb(z) }
}

object ByteArrayTile {
  def apply(arr: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    apply(arr, cols, rows, ByteConstantNoDataCellType)

  def apply(arr: Array[Byte], cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =
    cellType match {
      case ByteCellType =>
        new ByteRawArrayTile(arr, cols, rows)
      case ByteConstantNoDataCellType =>
        new ByteConstantNoDataArrayTile(arr, cols, rows)
      case udct: ByteUserDefinedNoDataCellType =>
        new ByteUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): ByteArrayTile =
    ofDim(cols, rows, ByteConstantNoDataCellType)

  def ofDim(cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =  cellType match {
    case ByteCellType =>
      new ByteRawArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case ByteConstantNoDataCellType =>
      new ByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)
    case udct: ByteUserDefinedNoDataCellType =>
      new ByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows), cols, rows, udct)
  }

  def empty(cols: Int, rows: Int): ByteArrayTile =
    empty(cols, rows, ByteConstantNoDataCellType)

  def empty(cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile = cellType match {
    case ByteCellType =>
      ofDim(cols, rows, cellType)
    case ByteConstantNoDataCellType =>
      fill(byteNODATA, cols, rows, cellType)
    case ByteUserDefinedNoDataCellType(nd) =>
      fill(nd, cols, rows, cellType)
  }

  def fill(v: Byte, cols: Int, rows: Int): ByteArrayTile =
    fill(v, cols, rows, ByteConstantNoDataCellType)

  def fill(v: Byte, cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile = cellType match {
    case ByteCellType =>
      new ByteRawArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case ByteConstantNoDataCellType =>
      new ByteConstantNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)
    case udct: ByteUserDefinedNoDataCellType =>
      new ByteUserDefinedNoDataArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows, udct)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    fromBytes(bytes, cols, rows, ByteConstantNoDataCellType)

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
