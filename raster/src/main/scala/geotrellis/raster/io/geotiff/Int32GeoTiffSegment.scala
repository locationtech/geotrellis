package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class NoDataInt32GeoTiffSegment(bytes: Array[Byte], noDataValue: Int) extends Int32GeoTiffSegment(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

class Int32GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 4

  def get(i: Int): Int = byteBuffer.getInt(i * 4)

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = i2d(get(i))

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case TypeByte => 
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2b(get(i)) }
        arr
      case TypeShort =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2s(get(i)) }
        arr.toArrayByte()
      case TypeInt =>
        bytes
      case TypeFloat =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2f(get(i)) }
        arr.toArrayByte()
      case TypeDouble =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(getInt(i))
    }
    val result = new Array[Byte](size * TypeInt.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2i(f(getDouble(i)))
    }
    val result = new Array[Byte](size * TypeInt.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(i, getInt(i))
    }
    val result = new Array[Byte](size * TypeInt.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2i(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * TypeInt.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }
}
