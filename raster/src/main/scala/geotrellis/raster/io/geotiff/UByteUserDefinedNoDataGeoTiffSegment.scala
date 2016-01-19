package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.compression._

import java.util.BitSet

import spire.syntax.cfor._

class UByteUserDefinedNoDataGeoTiffSegment(bytes: Array[Byte], val userDefinedIntNoDataValue: Byte)
    extends UByteGeoTiffSegment(bytes)
    with UserDefinedByteNoDataConversions {
  val userDefinedByteNoDataValue = userDefinedIntNoDataValue.toByte
  def getInt(i: Int): Int = udb2i(getRaw(i))
  def getDouble(i: Int): Double = udb2d(getRaw(i))

  protected def intToUByteOut(v: Int): Byte = i2udb(v)
  protected def doubleToUByteOut(v: Double): Byte = d2udb(v)

  protected def convertToConstantNoData(cellType: DataType with ConstantNoData): Array[Byte] =
    cellType match {
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2b(getRaw(i)) }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2ub(getRaw(i)) }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2s(getRaw(i)) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2us(getRaw(i)) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2i(getRaw(i)) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2f(getRaw(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = udb2d(getRaw(i)) }
        arr.toArrayByte()
    }

  protected def convertToUserDefinedNoData(cellType: DataType with UserDefinedNoData[_]): Array[Byte] =
    cellType match {
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == userDefinedByteNoDataValue) nd else v
        }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v ==  userDefinedByteNoDataValue) nd else v
        }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == userDefinedByteNoDataValue) nd else (v & 0xFF).toShort
        }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getRaw(i)
          arr(i) = if (v == userDefinedByteNoDataValue) nd else (v & 0xFF).toShort
        }
        arr.toArrayByte()
    }
}
