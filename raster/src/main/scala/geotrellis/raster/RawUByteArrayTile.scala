package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
final case class RawUByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeRawUByte

  def apply(i: Int) = array(i) & 0xFF
  def update(i: Int, z: Int) { array(i) = z.toByte }

  def toBytes: Array[Byte] = array.clone

  def copy = RawUByteArrayTile(array.clone, cols, rows)
}

object RawUByteArrayTile {
  def ofDim(cols: Int, rows: Int): RawUByteArrayTile =
    new RawUByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): RawUByteArrayTile =
    new RawUByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows)

  def fill(v: Byte, cols: Int, rows: Int): RawUByteArrayTile =
    new RawUByteArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): RawUByteArrayTile =
    RawUByteArrayTile(bytes.clone, cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): RawUByteArrayTile = {
    println("WARNING: Raw celltypes lack a NoData value; any default value will be ignored")
    fromBytes(bytes, cols, rows)
  }
}
