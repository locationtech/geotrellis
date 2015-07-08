package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.utils._

import spire.syntax.cfor._

class BitGeoTiffSegment(val bytes: Array[Byte], cols: Int, rows: Int) extends GeoTiffSegment {
  val size = cols * rows

  private val paddedCols = {
    val bytesWidth = (cols + 7) / 8
    bytesWidth * 8
  }

  def getInt(i: Int): Int = b2i(get(i))
  def getDouble(i: Int): Double = b2d(get(i))

  /** Creates a corrected index into the byte array that accounts for byte padding on rows */
  private def index(i: Int): Int = {
    val col = (i % cols)
    val row = (i / cols)

    (row * paddedCols) + col
  }

  def get(i: Int): Byte = {
    val i2 = index(i)
    ((invertByte(bytes(i2 >> 3)) >> (i2 & 7)) & 1).asInstanceOf[Byte]
  }

  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case TypeBit => bytes
      case TypeByte | TypeUByte =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = get(i) }
        arr
      case TypeShort | TypeUShort =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = b2s(get(i)) }
        arr.toArrayByte()
      case TypeInt =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case TypeFloat =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = b2f(get(i)) }
        arr.toArrayByte()
      case TypeDouble =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
    }

  def map(f: Int => Int): Array[Byte] = {
    val arr = bytes.clone
    val f0 = f(0) & 1
    val f1 = f(1) & 1

    if (f0 == 0 && f1 == 0) {
      cfor(0)(_ < size, _ + 1) { i => arr(i) = 0.toByte }
    } else if (f0 == 1 && f1 == 1) {
      cfor(0)(_ < size, _ + 1) { i => arr(i) = -1.toByte }
    } else if (f0 != 0 || f1 != 1) {
      // inverse (complement) of what we have now
      var i = 0
      val len = arr.size
      while(i < len) { arr(i) = (bytes(i) ^ -1).toByte ; i += 1 }
    }
    // If none of the above conditions, we just have the same data as we did before (which was cloned)

    arr
  }

  def mapDouble(f: Double => Double): Array[Byte] = 
    map(z => d2i(f(i2d(z))))

  def byteToBinaryString(b: Byte) = {
    val binaryStringBuilder = new StringBuilder()
    for(i <- 0 until 8) {
      binaryStringBuilder.append(if( ((0x80 >>> i) & b) == 0) '0' else '1')
    }
    binaryStringBuilder.toString
  }

  def mapWithIndex(f: (Int, Int) => Int): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val i = row * cols + col
        BitArrayTile.update(arr, index(i), f(i, getInt(i)))
      }
    }

    cfor(0)(_ < arr.size, _ + 1) { i =>
      arr(i) = invertByte(arr(i))
    }

    arr
  }

  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte] = {
    val arr = bytes.clone

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val i = row * cols + col
        BitArrayTile.updateDouble(arr, index(i), f(i, getDouble(i)))
      }
    }

    cfor(0)(_ < arr.size, _ + 1) { i =>
      arr(i) = invertByte(arr(i))
    }

    arr
  }
}
