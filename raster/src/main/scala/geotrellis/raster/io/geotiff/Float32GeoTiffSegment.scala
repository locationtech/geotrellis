package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._

import java.nio.ByteBuffer
import spire.syntax.cfor._

import java.util.BitSet

abstract class Float32GeoTiffSegment(val bytes: Array[Byte]) extends GeoTiffSegment {
  protected val buffer = ByteBuffer.wrap(bytes).asFloatBuffer

  val size: Int = bytes.size / 4

  def get(i: Int): Float = buffer.get(i)

  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  protected def intToFloatOut(v: Int): Float
  protected def doubleToFloatOut(v: Double): Float

  def map(f: Int => Int): Array[Byte] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToFloatOut(f(getInt(i)))
    }
    val result = new Array[Byte](size * FloatConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asFloatBuffer.put(arr)
    result
  }

  def mapDouble(f: Double => Double): Array[Byte] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = d2f(f(getDouble(i)))
    }
    val result = new Array[Byte](size * FloatConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asFloatBuffer.put(arr)
    result
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = intToFloatOut(f(i, getInt(i)))
    }
    val result = new Array[Byte](size * FloatConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asFloatBuffer.put(arr)
    result
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = Array.ofDim[Float](size)
    cfor(0)(_ < size, _ + 1) { i =>
      arr(i) = doubleToFloatOut(f(i, getDouble(i)))
    }
    val result = new Array[Byte](size * FloatConstantNoDataCellType.bytes)
    val bytebuff = ByteBuffer.wrap(result)
    bytebuff.asFloatBuffer.put(arr)
    result
  }
}
