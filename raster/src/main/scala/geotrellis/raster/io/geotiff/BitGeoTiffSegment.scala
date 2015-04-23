package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import spire.syntax.cfor._

class BitGeoTiffSegment(val bytes: Array[Byte], val size: Int, width: Int) extends GeoTiffSegment {
  private val paddedCols = {
    val bytesWidth = (width + 7) / 8
    bytesWidth * 8
  }

  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  def get(i: Int): Byte = {
    val col = (i % width)
    val row = (i / width)

    val i2 = (row * paddedCols) + col

    ((invertByte(bytes(i2 >> 3)) >> (i2 & 7)) & 1).asInstanceOf[Byte]
  }

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit => bytes
      case TypeByte =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i) }
        arr
      case TypeShort =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = b2s(get(i)) }
        arr.toArrayByte()
      case TypeInt =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case TypeFloat =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = b2f(get(i)) }
        arr.toArrayByte()
      case TypeDouble =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  private def update(arr: Array[Byte], i: Int, z: Int): Unit = {
    val div = i >> 3
    if ((z & 1) == 0) {
      // unset the nth bit
      arr(div) = (arr(div) & ~(1 << (i & 7))).toByte
    } else {
      // set the nth bit
      arr(div) = (arr(div) | (1 << (i & 7))).toByte
    }
  }

  def map(f: Int => Int): Array[Byte] = {
    val arr = bytes.clone
    val f0 = f(0) & 1
    val f1 = f(1) & 1

    if (f0 == 0 && f1 == 0) {
      cfor(0)(_ < size, _ + 1) { i => arr(i) = 0.toByte }
    } else if (f0 == 1 && f1 == 1) {
      cfor(0)(_ < size, _ + 1) { i => arr(i) = -1.toByte }
    } else if (f0 != 0 || f1 == 1) {
      // inverse (complement) of what we have now
      var i = 0
      val len = arr.size
      while(i < len) { arr(i) = (bytes(i) ^ -1).toByte ; i += 1 }
    }
    // If none of the above conditions, we just have the same data as we did before (which was cloned)

    arr
  }

  def mapDouble(f: Double => Double): Array[Byte] = 
    map(z => d2i(f(i2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      update(arr, i, f(i, getInt(i)))
    }
    arr
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      update(arr, i, d2i(f(i, getDouble(i))))
    }
    arr
  }
}
