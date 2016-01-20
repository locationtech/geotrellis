package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

class Float32UserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedFloatNoDataValue: Float)
    extends Float32GeoTiffSegment(bytes)
       with UserDefinedFloatNoDataConversions {
  def getInt(i: Int): Int = udf2i(get(i))
  def getDouble(i: Int): Double = udf2d(get(i))

  protected def intToFloatOut(v: Int): Float = i2f(v)
  protected def doubleToFloatOut(v: Double): Float = d2f(v)

  protected def convertToConstantNoData(cellType: DataType with ConstantNoData): Array[Byte] =
    cellType match {
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udf2b(get(i)) }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udf2ub(get(i)) }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udf2s(get(i)) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udf2us(get(i)) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udf2f(get(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  protected def convertToUserDefinedNoData(cellType: DataType with UserDefinedNoData[_]): Array[Byte] =
    cellType match {
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toByte
        }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toByte
        }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toShort
        }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toShort
        }
        arr.toArrayByte()
      case IntUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toInt
        }
        arr.toArrayByte()
      case FloatUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v
        }
        arr.toArrayByte()
      case DoubleUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = get(i)
          arr(i) = if (v == userDefinedFloatNoDataValue) nd else v.toDouble
        }
        arr.toArrayByte()
    }
}
