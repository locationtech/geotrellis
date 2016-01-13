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

trait UByteRawSegment {
  def get(i: Int): Int
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
}

trait UByteConstantNoDataSegment {
  def getRaw(i: Int): Byte
  def getInt(i: Int): Int = ub2i(getRaw(i))
  def getDouble(i: Int): Double = ub2i(getRaw(i))
}

trait UByteUserDefinedNoDataSegment {
  val noDataValue: Int

  def get(i: Int): Int
  def getInt(i: Int): Int = {
    val n = get(i);
    if (n == noDataValue) Int.MinValue else n
  }
  def getDouble(i: Int): Double = {
    val n = get(i);
    if (n == noDataValue) Double.NaN else n.toDouble
  }
}
