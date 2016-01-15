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

  protected def convertToUserDefinedNoData(cellType: UserDefinedNoDataCellType[_]): Array[Byte]
  protected def convertToConstantNoData(cellType: ConstantNoDataCellType): Array[Byte]

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case rct: RawCellType => rct match {
        case BitCellType =>
          val bs = new BitSet(size)
          cfor(0)(_ < size, _ + 1) { i => if ((get(i) & 1) == 0) { bs.set(i) } }
          bs.toByteArray()
        case ByteCellType | UByteCellType =>
          bytes
        case ShortCellType | UShortCellType =>
          val arr = Array.ofDim[Short](size)
          cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i).toShort }
          arr.toArrayByte()
      }
      case cct: ConstantNoDataCellType => convertToConstantNoData(cct)
      case udct: UserDefinedNoDataCellType[_] => convertToUserDefinedNoData(udct)
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
