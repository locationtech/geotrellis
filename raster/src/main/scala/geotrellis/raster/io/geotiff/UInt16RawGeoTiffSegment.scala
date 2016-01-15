package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import java.nio.ByteBuffer
import java.util.BitSet

import spire.syntax.cfor._


class UInt16RawGeoTiffSegment(bytes: Array[Byte]) extends UInt16GeoTiffSegment(bytes) {
  def getInt(i: Int): Int = get(i)
  def getDouble(i: Int): Double = get(i).toDouble
  // we want to preserve Int.MinValue results and this yields a slight performance boost
  override def mapDouble(f: Double => Double): Array[Byte] =
    map(z => f(z.toDouble).toInt)

  protected def intToUShortOut(v: Int): Short = v.toShort
  protected def doubleToUShortOut(v: Double): Short = v.toShort

  protected def convertToConstantNoData(cellType: ConstantNoDataCellType): Array[Byte] =
    cellType match {
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i).toByte }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i).toByte }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i) }
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
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i).toDouble }
        arr.toArrayByte()
    }

  protected def convertToUserDefinedNoData(cellType: UserDefinedNoDataCellType[_]): Array[Byte] =
    cellType match {
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i).toByte }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i).toByte }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i) }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getRaw(i) }
        arr.toArrayByte()
    }
}
