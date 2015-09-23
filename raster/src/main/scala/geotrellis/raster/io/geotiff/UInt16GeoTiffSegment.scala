package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class NoDataUInt16GeoTiffSegment(bytes: Array[Byte], noDataValue: Int) extends UInt16GeoTiffSegment(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

class UInt16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asShortBuffer

  val size: Int = bytes.size / 2

  def getRaw(i: Int): Short = buffer.get(i) // Get's the signed short, negative values are incorrect
  def get(i: Int): Int = buffer.get(i) & 0xFFFF

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = i2d(get(i))

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case TypeByte | TypeUByte => 
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2b(get(i)) }
        arr
      case TypeShort | TypeUShort  =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2s(get(i)) }
        arr.toArrayByte()
      case TypeInt =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i) }
        arr.toArrayByte()
      case TypeFloat =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2f(get(i)) }
        arr.toArrayByte()
      case TypeDouble =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  // NOTE: Maps to Int32 bytes.
  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = (f(getInt(i)) & 0xFFFF).toShort
    }
    val result = new Array[Byte](size * TypeShort.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = (d2i(f(getDouble(i))) & 0xFFFF).toShort
    }
    val result = new Array[Byte](size * TypeShort.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
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
