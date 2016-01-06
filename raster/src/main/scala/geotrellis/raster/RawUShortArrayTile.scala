package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
final case class RawUShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeRawUShort

  def apply(i: Int) = array(i) & 0xFFFF
  def update(i: Int, z: Int) { array(i) = if(isNoData(z)) 0.toShort else z.toShort }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = RawUShortArrayTile(array.clone, cols, rows)
}

object RawUShortArrayTile {
  def ofDim(cols: Int, rows: Int): RawUShortArrayTile =
    new RawUShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): RawUShortArrayTile =
    new RawUShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def fill(v: Short, cols: Int, rows: Int): RawUShortArrayTile =
    new RawUShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): RawUShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / TypeShort.bytes)
    shortBuffer.get(shortArray)

    RawUShortArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): RawUShortArrayTile = {
    println("WARNING: Raw celltypes lack a NoData value; any default value will be ignored")
    fromBytes(bytes, cols, rows)
  }
}
