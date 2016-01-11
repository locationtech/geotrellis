package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
final case class RawShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeRawShort

  def apply(i: Int) = array(i).toInt
  def update(i: Int, z: Int) { array(i) = z.toShort }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

object RawShortArrayTile {
  def fill(v: Short, cols: Int, rows: Int): RawShortArrayTile =
    new RawShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def ofDim(cols: Int, rows: Int): RawShortArrayTile =
    new RawShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): RawShortArrayTile =
    new RawShortArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): RawShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / TypeShort.bytes)
    shortBuffer.get(shortArray)

    RawShortArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): RawShortArrayTile = {
    println("WARNING: Raw celltypes lack a NoData value; any default value will be ignored")
    fromBytes(bytes, cols, rows)
  }
}

