package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

abstract class Float64GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asDoubleBuffer

  val size: Int = bytes.size / 8

  def get(i: Int): Double =
    buffer.get(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def intToDoubleOut(v: Int): Double
  protected def doubleToDoubleOut(v: Double): Double

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2d(f(getInt(i)))
    }
    val result = new Array[Byte](size * DoubleConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(getDouble(i))
    }
    val result = new Array[Byte](size * DoubleConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = i2d(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * DoubleConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Double](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = f(i, getDouble(i))
    }
    val result = new Array[Byte](size * DoubleConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asDoubleBuffer.put(arr)
    result
  }
}
