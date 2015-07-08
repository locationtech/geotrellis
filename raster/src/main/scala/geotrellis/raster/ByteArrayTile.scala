package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Byte] (each cell as a Byte).
 */
final case class ByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeByte

  def apply(i: Int) = b2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2b(z) }

  def toBytes: Array[Byte] = array.clone

  def copy = ArrayTile(array.clone, cols, rows)
}

object ByteArrayTile {
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
        arr(i) = bytes(i)
      }
      ByteArrayTile(arr, cols, rows)
    }
}

final case class NoDataByteArrayTile(array: Array[Byte], cols: Int, rows: Int, nd: Byte)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeByte

  val ndI = nd.toInt

  def apply(i: Int) = { val z = array(i).toInt ; if(z == ndI) NODATA else z }
  def update(i: Int, z: Int) { array(i) = if(z == nd) nd else z.toByte  }

  def toBytes: Array[Byte] = array.clone

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile =
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Byte](target.cols * target.rows).fill(byteNODATA)
        Resample(RasterExtent(current, cols, rows), target, new ByteBufferResampleAssign(ByteBuffer.wrap(array), resampled))
        NoDataByteArrayTile(resampled, target.cols, target.rows, nd)
      case _ =>
        Resample(this, current, target, method)
    }
}

object NoDataByteArrayTile {
  def ofDim(cols: Int, rows: Int, nd: Byte): NoDataByteArrayTile =
    new NoDataByteArrayTile(Array.ofDim[Byte](cols * rows), cols, rows, nd)

  def empty(cols: Int, rows: Int, nd: Byte): NoDataByteArrayTile =
    new NoDataByteArrayTile(Array.ofDim[Byte](cols * rows).fill(byteNODATA), cols, rows, nd)

  def fill(v: Byte, cols: Int, rows: Int, nd: Byte): NoDataByteArrayTile =
    new NoDataByteArrayTile(Array.ofDim[Byte](cols * rows).fill(v), cols, rows, nd)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, nd: Byte): NoDataByteArrayTile =
    NoDataByteArrayTile(bytes.clone, cols, rows, nd)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, nd: Byte, replaceNoData: Byte): NoDataByteArrayTile =
    fromBytes(bytes, cols, rows, nd)
}

final case class NoNoDataByteArrayTile(array: Array[Byte], cols: Int, rows: Int)
  extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeByte

  def apply(i: Int) = array(i).toInt
  def update(i: Int, z: Int) = array(i) = z.toByte

  def toBytes: Array[Byte] = array.clone

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile =
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Byte](target.cols * target.rows).fill(byteNODATA)
        Resample(RasterExtent(current, cols, rows), target, new ByteBufferResampleAssign(ByteBuffer.wrap(array), resampled))
        NoNoDataByteArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}
