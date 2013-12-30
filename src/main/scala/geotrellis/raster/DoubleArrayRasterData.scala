package geotrellis.raster

import geotrellis._
import java.nio.ByteBuffer

/**
 * RasterData based on Array[Double] (each cell as a Double).
 */
final case class DoubleArrayRasterData(array: Array[Double], cols: Int, rows: Int)
  extends MutableRasterData with DoubleBasedArray {
  def getType = TypeDouble
  def alloc(cols: Int, rows: Int) = DoubleArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i: Int) = array(i)
  def updateDouble(i: Int, z: Double) = array(i) = z
  def copy = DoubleArrayRasterData(array.clone, cols, rows)
  override def toArrayDouble = array.clone

  def toArrayByte: Array[Byte] = {
    val pixels = new Array[Byte](array.length * getType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asDoubleBuffer.put(array)
    pixels
  }

  def warp(current:RasterExtent,target:RasterExtent):RasterData = {
    val warped = Array.ofDim[Double](target.cols*target.rows).fill(Double.NaN)
    Warp[Double](current,target,array,warped)
    DoubleArrayRasterData(warped, target.cols, target.rows)
  }
}

object DoubleArrayRasterData {
  def ofDim(cols: Int, rows: Int) = 
    new DoubleArrayRasterData(Array.ofDim[Double](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = 
    new DoubleArrayRasterData(Array.ofDim[Double](cols * rows).fill(Double.NaN), cols, rows)

  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int) = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val doubleBuffer = byteBuffer.asDoubleBuffer()
    val doubleArray = new Array[Double](bytes.length / TypeDouble.bytes)
    doubleBuffer.get(doubleArray)

    DoubleArrayRasterData(doubleArray, cols, rows)
  }
}
