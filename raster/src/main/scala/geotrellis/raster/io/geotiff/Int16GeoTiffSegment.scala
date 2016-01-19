package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

abstract class Int16GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asShortBuffer

  val size: Int = bytes.size / 2

  def get(i: Int): Short = buffer.get(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def convertToUserDefinedNoData(cellType: DataType with UserDefinedNoData[_]): Array[Byte]
  protected def convertToConstantNoData(cellType: DataType with ConstantNoData): Array[Byte]

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteCellType | UByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = s2b(get(i)) }
        arr
      case ShortCellType | UShortCellType =>
        bytes
      case cct: ConstantNoData => convertToConstantNoData(cct)
      case udct: UserDefinedNoData[_] => convertToUserDefinedNoData(udct)
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2s(f(getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    map(z => d2i(f(i2d(z))))
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2s(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Short](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2s(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * ShortConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asShortBuffer.put(arr)
    result
  }
}
