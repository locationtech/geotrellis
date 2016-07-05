package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.util._
import geotrellis.raster._

import java.util.BitSet
import spire.syntax.cfor._

/**
 * Base trait of GeoTiffSegment
 */
trait GeoTiffSegment {
  def size: Int
  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  /** represents all of the bytes in the segment */
  def bytes: Array[Byte]

  def map(f: Int => Int): Array[Byte]
  def mapDouble(f: Double => Double): Array[Byte]
  def mapWithIndex(f: (Int, Int) => Int): Array[Byte]
  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte]

  /**
   * Converts the segment to the given CellType
   *
   * @param cellType: The desired [[CellType]] to convert to
   * @return An Array[Byte] that contains the new CellType values
   */
  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((getInt(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteCellType | UByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => getInt(i).toByte }
        arr
      case ShortCellType | UShortCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i).toShort }
        arr.toArrayByte()
      case IntCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i).toFloat }
        arr.toArrayByte()
      case DoubleCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2b(getInt(i)) }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2ub(getInt(i)) }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2s(getInt(i)) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2us(getInt(i)) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = d2f(getDouble(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toByte
        }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toByte
        }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toShort
        }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toShort
        }
        arr.toArrayByte()
      case IntUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v
        }
        arr.toArrayByte()
      case FloatUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getDouble(i)
          arr(i) = if (v == Double.NaN) nd else v.toFloat
        }
        arr.toArrayByte()
      case DoubleUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getDouble(i)
          arr(i) = if (v == Double.NaN) nd else v
        }
        arr.toArrayByte()
    }
}
