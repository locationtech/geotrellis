package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._

import java.nio.ByteBuffer

import spire.syntax.cfor._

class ByteValueWrapper(bytes: Array[Byte]) {
  val size: Int = bytes.size
  def toArray: Array[Byte] = bytes.clone
  def transfer(arr: Array[Byte])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

  def get(i: Int): Byte = bytes(i)
}

class NoDataByteValueWrapper(bytes: Array[Byte], noDataValue: Byte) extends ByteValueWrapper(bytes) {
  override
  def get(i: Int): Byte = {
    val v = super.get(i)
    if(v == noDataValue) { byteNODATA }
    else { v }
  }
}

abstract class UInt16ValueWrapper(bytes: Array[Byte]) {
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

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Int])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

}

class NoDataUInt16ValueWrapper(bytes: Array[Byte], noDataValue: Int) extends UInt16ValueWrapper(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

abstract class Int16ValueWrapper(bytes: Array[Byte]) {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 2

  def toArray: Array[Short] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Short])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

  def get(i: Int): Short = byteBuffer.getShort(i * 2)
}

class NoDataInt16ValueWrapper(bytes: Array[Byte], noDataValue: Short) extends Int16ValueWrapper(bytes) {
  override
  def get(i: Int): Short = {
    val v = super.get(i)
    if(v == noDataValue) { shortNODATA }
    else { v }
  }
}

abstract class UInt32ValueWrapper(bytes: Array[Byte]) {
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

  def toArray: Array[Float] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Float])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

}

class NoDataUInt32ValueWrapper(bytes: Array[Byte], noDataValue: Float) extends UInt32ValueWrapper(bytes) {
  override
  def get(i: Int): Float = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }
  }
}

abstract class Int32ValueWrapper(bytes: Array[Byte]) {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 4

  def toArray: Array[Int] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Int])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

  def get(i: Int): Int = byteBuffer.getInt(i * 4)
}

class NoDataInt32ValueWrapper(bytes: Array[Byte], noDataValue: Int) extends Int32ValueWrapper(bytes) {
  override
  def get(i: Int): Int = {
    val v = super.get(i)
    if(v == noDataValue) { NODATA }
    else { v }
  }
}

abstract class Float32ValueWrapper(bytes: Array[Byte]) {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 4

  def toArray: Array[Float] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Float])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

  def get(i: Int): Float = byteBuffer.getFloat(i * 4)
}

class NoDataFloat32ValueWrapper(bytes: Array[Byte], noDataValue: Float) extends Float32ValueWrapper(bytes) {
  override
  def get(i: Int): Float = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }

  }
}

abstract class Float64ValueWrapper(bytes: Array[Byte]) {
  protected val byteBuffer = ByteBuffer.wrap(bytes)

  val size: Int = bytes.size / 8

  def toArray: Array[Double] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = get(i)
    }
    arr
  }

  def transfer(arr: Array[Double])(f: Int => Int): Unit =
    cfor(0)(_ < size, _ + 1) { i =>
      arr(f(i)) = get(i)
    }

  def get(i: Int): Double = byteBuffer.getDouble(i * 8)
}

class NoDataFloat64ValueWrapper(bytes: Array[Byte], noDataValue: Float) extends Float32ValueWrapper(bytes) {
  override
  def get(i: Int): Float = {
    val v = super.get(i)
    if(v == noDataValue) { Float.NaN }
    else { v }

  }
}

