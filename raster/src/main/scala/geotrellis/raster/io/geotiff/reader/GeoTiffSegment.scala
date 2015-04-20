package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.geotiff._

import java.nio.ByteBuffer
import spire.syntax.cfor._

trait GeoTiffSegment {
  def size: Int
  def getInt(i: Int): Int
  def getDouble(i: Int): Double

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

class BitGeoTiffSegment(val bytes: Array[Byte], val size: Int) extends GeoTiffSegment {
  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  def get(i: Int): Byte = ((bytes(i >> 3) >> (i & 7)) & 1).asInstanceOf[Byte]
}

class ByteGeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  val size: Int = bytes.size

  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  def get(i: Int): Byte = bytes(i)
}

class NoDataByteGeoTiffSegment(bytes: Array[Byte], noDataValue: Byte) extends ByteGeoTiffSegment(bytes) {
  override
  def get(i: Int): Byte = {
    val v = super.get(i)
    if(v == noDataValue) { byteNODATA }
    else { v }
  }
}

class UInt16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  private val buffer = Array.ofDim[Byte](4)
  buffer(0) = 0.toByte
  buffer(1) = 0.toByte

  val size: Int = bytes.size / 2

  def get(i: Int): Int = {
    val bi = i * 2
    buffer(2) = bytes(bi)
    buffer(3) = bytes(bi + 1)
    ByteBuffer.wrap(buffer, 0, 4).getInt
  }

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = i2d(get(i))
}

class NoDataUInt16GeoTiffSegment(bytes: Array[Byte], noDataValue: Int) extends UInt16GeoTiffSegment(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

class Int16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asShortBuffer

  val size: Int = bytes.size / 2

  def get(i: Int): Short = buffer.get(i)

  def getInt(i: Int): Int = s2i(get(i))
  def getDouble(i: Int): Double = s2d(get(i))
}

class NoDataInt16GeoTiffSegment(bytes: Array[Byte], noDataValue: Short) extends Int16GeoTiffSegment(bytes) {
  override
  def get(i: Int): Short = {
    val v = super.get(i)
    if(v == noDataValue) { shortNODATA }
    else { v }
  }
}

class UInt32GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  private val buffer = Array.ofDim[Byte](8)
  buffer(0) = 0.toByte
  buffer(1) = 0.toByte
  buffer(2) = 0.toByte
  buffer(3) = 0.toByte

  val size: Int = bytes.size / 4

  def get(i: Int): Float = {
    val bi = i * 4
    buffer(4) = bytes(bi)
    buffer(5) = bytes(bi + 1)
    buffer(6) = bytes(bi + 2)
    buffer(7) = bytes(bi + 3)
    ByteBuffer.wrap(buffer, 0, 8).getLong.toFloat
  }

  def getInt(i: Int): Int = f2i(get(i))
  def getDouble(i: Int): Double = f2d(get(i))
}

class NoDataUInt32GeoTiffSegment(bytes: Array[Byte], noDataValue: Float) extends UInt32GeoTiffSegment(bytes) {
  override
  def get(i: Int): Float = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }
  }
}

class Int32GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 4

  def get(i: Int): Int = byteBuffer.getInt(i * 4)

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = i2d(get(i))
}

class NoDataInt32GeoTiffSegment(bytes: Array[Byte], noDataValue: Int) extends Int32GeoTiffSegment(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

class Float32GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 4

  def get(i: Int): Float = byteBuffer.getFloat(i * 4)

  def getInt(i: Int): Int = f2i(get(i))
  def getDouble(i: Int): Double = f2d(get(i))
}

class NoDataFloat32GeoTiffSegment(bytes: Array[Byte], noDataValue: Float) extends Float32GeoTiffSegment(bytes) {
  override
  def get(i: Int): Float = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }
  }
}

class Float64GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 8

  def get(i: Int): Double = byteBuffer.getDouble(i * 8)

  def getInt(i: Int): Int = d2i(get(i))
  def getDouble(i: Int): Double = get(i)
}

class NoDataFloat64GeoTiffSegment(bytes: Array[Byte], noDataValue: Double) extends Float64GeoTiffSegment(bytes) {
  override
  def get(i: Int): Double = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }
  }
}
