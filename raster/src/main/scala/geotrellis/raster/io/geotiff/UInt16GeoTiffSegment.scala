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

  protected def intToShort(v: Int): Short
  protected def intToDouble(v: Int): Double
  protected def doubleToInt(v: Double): Int

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
  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToShort(f(getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => doubleToInt(f(intToDouble(z))))

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
      arr(i) = doubleToInt(f(i, getDouble(i)))
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

  protected def intToShort(v: Int): Short = v.toShort
  protected def intToDouble(v: Int): Double = v.toDouble
  protected def doubleToInt(v: Double): Int = v.toInt
}

trait UInt16ConstantNoDataSegment {
  val size: Int
  def getRaw(i: Int): Short

  def getInt(i: Int): Int = us2i(getRaw(i))
  def getDouble(i: Int): Double = us2d(getRaw(i))

  protected def intToShort(v: Int): Short = i2us(v)
  protected def intToDouble(v: Int): Double = i2d(v)
  protected def doubleToInt(v: Double): Int = d2i(v)
}

trait UInt16UserDefinedNoDataSegment extends UserDefinedIntNoDataConversions {
  val size: Int

  def get(i: Int): Int

  def getInt(i: Int): Int = udi2i(get(i))
  def getDouble(i: Int): Double = udi2d(get(i))

  protected def intToShort(v: Int): Short = i2uds(v)
  protected def intToDouble(v: Int): Double = udi2d(v)
  protected def doubleToInt(v: Double): Int = udd2i(v)
}
