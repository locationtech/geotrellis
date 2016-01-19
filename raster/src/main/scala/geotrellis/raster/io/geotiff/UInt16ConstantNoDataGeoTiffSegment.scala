package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._

class UInt16ConstantNoDataGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = us2i(getRaw(i))
  def getDouble(i: Int): Double = us2d(getRaw(i))

  protected def intToUShortOut(v: Int): Short = i2us(v)
  protected def doubleToUShortOut(v: Double): Short = d2us(v)

  protected def convertToConstantNoData(cellType: DataType with ConstantNoData): Array[Byte] =
    cellType match {
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2b(getRaw(i)) }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2ub(getRaw(i)) }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2s(getRaw(i)) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2i(getRaw(i)) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2f(getRaw(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = us2d(getRaw(i)) }
        arr.toArrayByte()
    }

  protected def convertToUserDefinedNoData(cellType: DataType with UserDefinedNoData[_]): Array[Byte] =
    cellType match {
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == 0.toShort) nd else v.toByte
        }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == 0.toShort) nd else v.toByte
        }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == 0.toShort) nd else v
        }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == 0.toShort) nd else v
        }
        arr.toArrayByte()
    }
}
