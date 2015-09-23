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

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): UByteArrayTile = {
    println("Unsigned bytes (such as those most commonly used in byte-based geotiffs) lack a NoData value; any default value will be ignored")
    fromBytes(bytes, cols, rows)
  }
}
