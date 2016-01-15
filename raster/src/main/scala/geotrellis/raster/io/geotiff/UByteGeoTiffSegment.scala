package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

abstract class UByteGeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  val size: Int = bytes.size

  def get(i: Int): Int = bytes(i) & 0xFF
  def getRaw(i: Int): Byte = bytes(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def intToUByteOut(v: Int): Byte
  protected def doubleToUByteOut(v: Double): Byte

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteConstantNoDataCellType | UByteConstantNoDataCellType | ByteCellType | UByteCellType =>
        bytes
      case ShortConstantNoDataCellType | UShortConstantNoDataCellType | ShortCellType | UShortCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2s(get(i)) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2f(get(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = bytes.clone
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2b(f(get(i)))
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

class UByteRawGeoTiffSegment(bytes: Array[Byte]) extends UByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUByteOut(v: Int): Byte = v.toByte
  protected def doubleToUByteOut(v: Double): Byte = v.toByte
}

class UByteConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends UByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = ub2i(getRaw(i))
  def getDouble(i: Int): Double = ub2i(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2ub(v)
  protected def doubleToUByteOut(v: Double): Byte = d2ub(v)
}

class UByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Byte)
    extends UByteGeoTiffSegment(bytes)
    with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  def getInt(i: Int): Int = udb2i(getRaw(i))
  def getDouble(i: Int): Double = udb2d(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToUByteOut(v: Double): Byte = d2udb(v)
}
