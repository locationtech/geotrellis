package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float64GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asDoubleBuffer

  val size: Int = bytes.size / 8

  def get(i: Int): Double =
    buffer.get(i)

  def getInt(i: Int): Int = d2i(get(i))
  def getDouble(i: Int): Double = get(i)

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((getInt(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case TypeByte => 
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = d2b(get(i)) }
        arr
      case TypeShort =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = d2s(get(i)) }
        arr.toArrayByte()
      case TypeInt =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case TypeFloat =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = d2f(get(i)) }
        arr.toArrayByte()
      case TypeDouble =>
        bytes
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2d(f(getInt(i)))
    }
    val result = new Array[Byte](size * TypeDouble.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(getDouble(i))
    }
    val result = new Array[Byte](size * TypeDouble.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2d(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * TypeDouble.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(i, getDouble(i))
    }
    val result = new Array[Byte](size * TypeDouble.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }
}

class NoDataFloat64GeoTiffSegment(bytes: Array[Byte], noDataValue: Double) extends Float64GeoTiffSegment(bytes) {
  override
  def get(i: Int): Double = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }
  }
}
