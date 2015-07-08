package geotrellis.raster

import geotrellis.raster.interpolation._
import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
final case class UShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeUShort

  def apply(i: Int) = array(i) & 0xFFFF
  def update(i: Int, z: Int) { array(i) = z.toShort }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = UShortArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile = 
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Short](target.cols * target.rows).fill(shortNODATA)
        Resample[Short](RasterExtent(current, cols, rows), target, array, resampled)
        ShortArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}

object UShortArrayTile {
  def ofDim(cols: Int, rows: Int): UShortArrayTile = 
    new UShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): UShortArrayTile = 
    new UShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def fill(v: Short, cols: Int, rows: Int): UShortArrayTile =
    new UShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): UShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / TypeShort.bytes)
    shortBuffer.get(shortArray)

    UShortArrayTile(shortArray, cols, rows)
  }

  // def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): UShortArrayTile = 
  //   if(isNoData(replaceNoData))
  //     fromBytes(bytes, cols, rows)
  //   else {
  //     val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
  //     val shortBuffer = byteBuffer.asShortBuffer()
  //     val len = bytes.length / TypeShort.bytes
  //     val shortArray = new Array[Short](len)
  //     cfor(0)(_ < len, _ + 1) { i =>
  //       val v = shortBuffer.get(i)
  //       if(v == replaceNoData)
  //         shortArray(i) = shortNODATA
  //       else
  //         shortArray(i) = v
  //     }

  //     UShortArrayTile(shortArray, cols, rows)
  //   }
}
