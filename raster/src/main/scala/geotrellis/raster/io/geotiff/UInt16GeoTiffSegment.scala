package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._


abstract class UInt16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asShortBuffer

  val size: Int = bytes.size / 2

  def get(i: Int): Int = buffer.get(i) & 0xFFFF
  def getRaw(i: Int): Short = buffer.get(i) // Gets the signed short

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteConstantNoDataCellType | UByteConstantNoDataCellType | ByteCellType | UByteCellType | ByteUserDefinedNoDataCellType(_) | UByteUserDefinedNoDataCellType(_) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i).toByte }
        arr
      case ShortConstantNoDataCellType | UShortConstantNoDataCellType | ShortCellType | UShortCellType | ShortUserDefinedNoDataCellType(_) | UShortUserDefinedNoDataCellType(_) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i).toShort }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i).toFloat }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i).toDouble }
        arr.toArrayByte()
    }

  // NOTE: Maps to Int32 bytes.
  def map(f: Int => Int): Array[Byte]

  def mapDouble(f: Double => Double): Array[Byte]

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte]

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(i, getInt(i))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }
}

trait UInt16RawSegment {
  val size: Int
  def get(i: Int): Int

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(get(i)).toByte
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }
}

trait UInt16ConstantNoDataSegment {
  val size: Int
  def get(i: Int): Int
  def getRaw(i: Int): Short

  def getInt(i: Int): Int = us2i(getRaw(i))
  def getDouble(i: Int): Double = us2i(getRaw(i))

  // NOTE: Maps to Int32 bytes.
  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2ub(f(get(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => d2i(f(i2d(z))))

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2i(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }

}

trait UInt16UserDefinedNoDataSegment extends UserDefinedNoDataConversions {
  val size: Int

  def get(i: Int): Int

  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = udi2d(get(i))

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2uds(f(getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => d2udi(f(udi2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(i, getInt(i))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Int](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2uds(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asIntBuffer.put(arr)
    result
  }
}
