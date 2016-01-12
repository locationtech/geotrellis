package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.raster.resample._

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
final case class DynamicByteArrayTile(array: Array[Byte], cols: Int, rows: Int, val cellType: DynamicCellType)
  extends MutableArrayTile with DynamicIntBasedArrayTile {

  def apply(i: Int) = {
    val n = array(i)
    if (n == cellType.noDataValue.toByte) NODATA else n
  }
  def update(i: Int, z: Int) { array(i) = i2b(z) }

  def toBytes: Array[Byte] = array.clone

  def copy = ArrayTile(array.clone, cols, rows)
}

object DynamicByteArrayTile {
  def ofDim(cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows).fill(byteNODATA), cols, rows)

  def fill(v: Byte, cols: Int, rows: Int): ByteArrayTile =
    new ByteArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ByteArrayTile =
    ByteArrayTile(bytes.clone, cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): ByteArrayTile =
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val arr = bytes.clone
      cfor(0)(_ < arr.size, _ + 1) { i =>
        val v = bytes(i)
        if(v == replaceNoData)
          arr(i) = byteNODATA
        else
          arr(i) = v
      }
      ByteArrayTile(arr, cols, rows)
    }
}
