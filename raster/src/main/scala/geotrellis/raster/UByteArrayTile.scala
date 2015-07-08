package geotrellis.raster

import geotrellis.raster.interpolation._
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

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile =
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Byte](target.cols * target.rows).fill(byteNODATA)
        Resample(RasterExtent(current, cols, rows), target, new ByteBufferResampleAssign(ByteBuffer.wrap(array), resampled))
        ByteArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
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

  // def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Byte): UByteArrayTile =
  //   if(isNoData(replaceNoData))
  //     fromBytes(bytes, cols, rows)
  //   else {
  //     val arr = bytes.clone
  //     cfor(0)(_ < arr.size, _ + 1) { i =>
  //       val v = bytes(i)
  //       if(v == replaceNoData)
  //         arr(i) = byteNODATA
  //       arr(i) = bytes(i)
  //     }
  //     ByteArrayTile(arr, cols, rows)
  //   }
}
