package geotrellis.raster

import java.nio.ByteBuffer

import geotrellis.NODATA
import geotrellis.TypeInt

/**
 * RasterData based on Array[Int] (each cell as an Int).
 */
final case class IntArrayRasterData(array: Array[Int], cols: Int, rows: Int) extends MutableRasterData with IntBasedArray {
  def getType = TypeInt
  def alloc(cols: Int, rows: Int) = IntArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i: Int) = array(i)
  def update(i: Int, z: Int) { array(i) = z }
  def copy = IntArrayRasterData(array.clone, cols, rows)
  override def toArray = array.clone

  def toArrayByte: Array[Byte] = {
    val pixels = new Array[Byte](array.length * getType.bytes)
    val bytebuff = ByteBuffer.wrap(pixels)
    bytebuff.asIntBuffer.put(array)
    pixels
  }
}

object IntArrayRasterData {
  def ofDim(cols: Int, rows: Int) = new IntArrayRasterData(Array.ofDim[Int](cols * rows), cols, rows)
  def empty(cols: Int, rows: Int) = new IntArrayRasterData(Array.fill[Int](cols * rows)(NODATA), cols, rows)
 
  def fromArrayByte(bytes: Array[Byte], cols: Int, rows: Int) = {
    val byteBuffer = ByteBuffer.wrap(bytes, 0, bytes.length)
    val intBuffer = byteBuffer.asIntBuffer()
    val intArray = new Array[Int](bytes.length / TypeInt.bytes)
    intBuffer.get(intArray)

    IntArrayRasterData(intArray, cols, rows)
  }
}
