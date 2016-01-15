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

  protected def intToUShortOut(v: Int): Short
  protected def doubleToUShortOut(v: Double): Short

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

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToUShortOut(f(getInt(i)))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => d2i(f(i2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToUShortOut(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = doubleToUShortOut(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * IntConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }
}

class UInt16RawGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
  // we want to preserve Int.MinValue results and this yields a slight performance boost
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUShortOut(v: Int): Short = v.toShort
  protected def doubleToUShortOut(v: Double): Short = v.toShort
}

class UInt16ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = us2i(getRaw(i))
  def getDouble(i: Int): Double = us2d(getRaw(i))

  protected def intToUShortOut(v: Int): Short = i2us(v)
  protected def doubleToUShortOut(v: Double): Short = d2us(v)
}

class UInt16UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedShortNoDataValue: Short)
    extends UInt16GeoTiffSegment(bytes)
       with UserDefinedShortNoDataConversions {

  def getInt(i: Int): Int = uds2i(getRaw(i))
  def getDouble(i: Int): Double = uds2d(getRaw(i))

  protected def intToUShortOut(v: Int): Short = i2uds(v)
  protected def doubleToUShortOut(v: Double): Short = d2uds(v)
}
