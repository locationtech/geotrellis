package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
final case class UByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeUByte

  def apply(i: Int) = array(i) & 0xFF
  def update(i: Int, z: Int) { array(i) = z.toByte }

  def toBytes: Array[Byte] = array.clone

  def copy = UByteArrayTile(array.clone, cols, rows)
}

object UByteArrayTile {
  def ofDim(cols: Int, rows: Int): UByteArrayTile =
    new UByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): UByteArrayTile =
    new UByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def fill(v: Byte, cols: Int, rows: Int): UByteArrayTile =
    new UByteArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UByteArrayTile =
    UByteArrayTile(bytes.clone, cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): UByteArrayTile =
    if(isNoData(replaceNoData))
    fromBytes(bytes, cols, rows)
    else {
      val arr = bytes.clone
      cfor(0)(_ < arr.size, _ + 1) { i =>
        val v = bytes(i)
        if(v == replaceNoData)
        arr(i) = byteNODATA
        arr(i) = bytes(i)
      }
      UByteArrayTile(arr, cols, rows)
    }
}
