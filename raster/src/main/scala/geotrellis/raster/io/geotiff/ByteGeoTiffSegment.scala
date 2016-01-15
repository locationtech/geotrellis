package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._


abstract class ByteGeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  val size: Int = bytes.size

  def get(i: Int): Byte = bytes(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def intToByteOut(v: Int): Byte
  protected def doubleToByteOut(v: Double): Byte

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
      arr(i) = intToByteOut(f(getInt(i)))
    }
    arr
  }

  def mapDouble(f: Double => Double): Array[Byte] =
    map(z => d2i(f(i2d(z))))

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToByteOut(f(i, getInt(i)))
    }
    arr
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = doubleToByteOut(f(i, getDouble(i)))
    }
    arr
  }
}

class ByteRawGeoTiffSegment(bytes: Array[Byte]) extends ByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i).toInt
  def getDouble(i: Int): Double = get(i).toDouble
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToByteOut(v: Int): Byte = v.toByte
  protected def doubleToByteOut(v: Double): Byte = v.toByte

}

class ByteConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends ByteGeoTiffSegment(bytes) {
  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2i(get(i))

  protected def intToByteOut(v: Int): Byte = i2b(v)
  protected def doubleToByteOut(v: Double): Byte = d2b(v)
}

class ByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedByteNoDataValue: Byte)
    extends ByteGeoTiffSegment(bytes) with UserDefinedByteNoDataConversions {
  def getInt(i: Int): Int = udb2i(get(i))
  def getDouble(i: Int): Double = udb2d(get(i))

  protected def intToByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToByteOut(v: Double): Byte = d2udb(v)
}
