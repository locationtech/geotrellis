package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.resample._

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
abstract class ByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {
  val cellType: ByteCells with NoDataHandling
  def apply(i: Int): Int
  def update(i: Int, z: Int)

  def toBytes: Array[Byte] = array.clone
  def copy: ByteArrayTile = ArrayTile(array.clone, cols, rows)
}

final case class ByteRawArrayTile(array: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(array, cols, rows) {
  val cellType = ByteCellType
  def apply(i: Int): Int = array(i).toInt
  def update(i: Int, z: Int) { array(i) = z.toByte }
}

final case class ByteConstantNoDataArrayTile(array: Array[Byte], val cols: Int, val rows: Int)
    extends ByteArrayTile(array, cols, rows) {
  val cellType = ByteConstantNoDataCellType
  def apply(i: Int): Int = b2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2b(z) }
}

final case class ByteUserDefinedNoDataArrayTile(array: Array[Byte], val cols: Int, val rows: Int, val cellType: ByteUserDefinedNoDataCellType)
    extends ByteArrayTile(array, cols, rows)
       with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = cellType.noDataValue
  def apply(i: Int): Int = udb2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2udb(z) }
}

object ByteArrayTile {
  def apply(arr: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    new ByteConstantNoDataArrayTile(arr, cols, rows)

  def apply(arr: Array[Byte], cols: Int, rows: Int, cellType: CellType with ByteCells): ByteArrayTile =
    cellType match {
      case ByteCellType =>
        new ByteRawArrayTile(arr, cols, rows)
      case ByteConstantNoDataCellType =>
        new ByteConstantNoDataArrayTile(arr, cols, rows)
      case udct @ ByteUserDefinedNoDataCellType(_) =>
        new ByteUserDefinedNoDataArrayTile(arr, cols, rows, udct)
    }

  def ofDim(cols: Int, rows: Int): ByteArrayTile =
    new ByteRawArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ByteArrayTile =
    new ByteRawArrayTile(Array.ofDim[Byte](cols * rows).fill(byteNODATA), cols, rows)

  def fill(v: Byte, cols: Int, rows: Int): ByteArrayTile =
    new ByteRawArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, cellType: ByteCells with NoDataHandling): ByteArrayTile =
    cellType match {
      case ByteCellType => new ByteRawArrayTile(bytes.clone, cols, rows)
      case ByteConstantNoDataCellType => new ByteConstantNoDataArrayTile(bytes.clone, cols, rows)
      case udct @ ByteUserDefinedNoDataCellType(_) => new ByteUserDefinedNoDataArrayTile(bytes.clone, cols, rows, udct)
    }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    fromBytes(bytes, cols, rows, ByteConstantNoDataCellType)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): ByteArrayTile =
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows, ByteConstantNoDataCellType)
    else {
      val arr = bytes.clone
      cfor(0)(_ < arr.size, _ + 1) { i =>
        val v = bytes(i)
        if(v == replaceNoData)
          arr(i) = byteNODATA
        else
          arr(i) = v
      }
      ByteRawArrayTile(arr, cols, rows)
    }
}
