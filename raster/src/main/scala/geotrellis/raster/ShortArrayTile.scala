package geotrellis.raster

import geotrellis.vector.Extent

import spire.syntax.cfor._
import java.nio.ByteBuffer

/**
 * ArrayTile based on Array[Short] (each cell as a Short).
 */
final case class ShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeShort

  def apply(i: Int) = s2i(array(i))
  def update(i: Int, z: Int) { array(i) = i2s(z) }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)
}

object ShortArrayTile {
  def ofDim(cols: Int, rows: Int): ShortArrayTile = 
    new ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

  def empty(cols: Int, rows: Int): ShortArrayTile = 
    new ShortArrayTile(Array.ofDim[Short](cols * rows).fill(shortNODATA), cols, rows)

  def fill(v: Short, cols: Int, rows: Int): ShortArrayTile =
    new ShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int): ShortArrayTile = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val shortBuffer = byteBuffer.asShortBuffer()
    val shortArray = new Array[Short](bytes.length / TypeShort.bytes)
    shortBuffer.get(shortArray)

    ShortArrayTile(shortArray, cols, rows)
  }

  def fromBytes(bytes: Array[Byte], cols: Int, rows: Int, replaceNoData: Short): ShortArrayTile = 
    if(isNoData(replaceNoData))
      fromBytes(bytes, cols, rows)
    else {
      val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
      val shortBuffer = byteBuffer.asShortBuffer()
      val len = bytes.length / TypeShort.bytes
      val shortArray = new Array[Short](len)
      cfor(0)(_ < len, _ + 1) { i =>
        val v = shortBuffer.get(i)
        if(v == replaceNoData)
          shortArray(i) = shortNODATA
        else
          shortArray(i) = v
      }

      ShortArrayTile(shortArray, cols, rows)
    }
}

final case class NoDataShortArrayTile(array: Array[Short], cols: Int, rows: Int, nd: Short)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeShort

  def apply(i: Int) = { val z = array(i) ; if(z == nd) NODATA else z.toInt }
  def update(i: Int, z: Int) { array(i) = if(z == NODATA) nd else z.toShort }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile = 
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Short](target.cols * target.rows).fill(nd)
        Resample[Short](RasterExtent(current, cols, rows), target, array, resampled)
        NoNoDataShortArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}

object NoDataShortArrayTile {
  def fill(v: Short, cols: Int, rows: Int, nd: Short): NoDataShortArrayTile =
    new NoDataShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows, nd)

}


final case class NoNoDataShortArrayTile(array: Array[Short], cols: Int, rows: Int)
    extends MutableArrayTile with IntBasedArrayTile {

  val cellType = TypeShort

  def apply(i: Int) = array(i).toInt
  def update(i: Int, z: Int) { array(i) = z.toShort }

  def toBytes: Array[Byte] = {
    val pixels = new Array[Byte](array.length * cellType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asShortBuffer.put(array)
    pixels
  }

  def copy = ArrayTile(array.clone, cols, rows)

  def resample(current: Extent, target: RasterExtent, method: InterpolationMethod): ArrayTile = 
    method match {
      case NearestNeighbor =>
        val resampled = Array.ofDim[Short](target.cols * target.rows).fill(shortNODATA)
        Resample[Short](RasterExtent(current, cols, rows), target, array, resampled)
        NoNoDataShortArrayTile(resampled, target.cols, target.rows)
      case _ =>
        Resample(this, current, target, method)
    }
}

object NoNoDataShortArrayTile {
  def fill(v: Short, cols: Int, rows: Int): NoNoDataShortArrayTile =
    new NoNoDataShortArrayTile(Array.ofDim[Short](cols * rows).fill(v), cols, rows)

}
