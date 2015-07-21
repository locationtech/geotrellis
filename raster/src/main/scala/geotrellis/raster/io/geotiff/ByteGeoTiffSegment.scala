package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class NoDataByteGeoTiffSegment(bytes: Array[Byte], noDataValue: Byte) extends ByteGeoTiffSegment(bytes) {
  override
  def get(i: Int): Byte = {
    val v = super.get(i)
    if(v == noDataValue) { byteNODATA }
    else { v }
  }
}

class ByteGeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  val size: Int = bytes.size

  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  def get(i: Int): Byte = bytes(i)

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case TypeByte => 
        bytes
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

  def map(f: Int => Int): Array[Byte] = {
    val arr = bytes.clone
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2b(f(getInt(i)))
    }
    arr
  }

  def mapDouble(f: Double => Double): Array[Byte] = 
    map(z => d2i(f(i2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2b(f(i, getInt(i)))
    }
    arr
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2b(f(i, getDouble(i)))
    }
    arr
  }
}
