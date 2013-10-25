package geotrellis.raster

import geotrellis._

/**
 * RasterData based on an Array[Byte] as a bitmask; values are 0 and 1.
 * Thus, there are 8 boolean (0/1) values per byte in the array. For example,
 * Array(11, 9) corresponds to (0 0 0 0 1 0 1 1), (0 0 0 0 1 0 0 1) which
 * means that we have 5 cells set to 1 and 11 cells set to 0.
 *
 * Note that unlike the other array-based raster data objects we need to be
 * explicitly told our size, since length=7 and length=8 will both need to
 * allocate an Array[Byte] with length=1.
 */
final case class BitArrayRasterData(array: Array[Byte], cols: Int, rows: Int)
  extends MutableRasterData with IntBasedArray {
  val size = cols * rows

  // i >> 3 is the same as i / 8 but faster
  // i & 7 is the same as i % 8 but faster
  // i & 1 is the same as i % 2 but faster
  // ~3 -> -4, that is 00000011 -> 11111100
  // 3 | 9 -> 11, that is 00000011 | 00001001 -> 00001011
  // 3 & 9 -> 1,  that is 00000011 & 00001001 -> 00000001
  // 3 ^ 9 -> 10, that is 00000011 ^ 00001001 -> 00001010
  assert(array.length == (size + 7) / 8)
  def getType = TypeBit
  def alloc(cols: Int, rows: Int) = BitArrayRasterData.ofDim(cols, rows)
  def length = size
  def apply(i: Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]
  def update(i: Int, z: Int): Unit = {
    val div = i >> 3
    if ((z & 1) == 0) {
      // unset the nth bit
      array(div) = (array(div) & ~(1 << (i & 7))).toByte
    } else {
      // set the nth bit
      array(div) = (array(div) | (1 << (i & 7))).toByte
    }
  }
  def copy = BitArrayRasterData(array.clone, cols, rows)

  override def map(f: Int => Int) = {
    val f0 = f(0) & 1
    val f1 = f(1) & 1
    val arr = if (f0 == 0 && f1 == 0) {
      // array of all zeros
      Array.ofDim[Byte](array.length)
    } else if (f0 == 1 && f1 == 1) {
      // array of all ones
      Array.fill[Byte](array.length)(-1.asInstanceOf[Byte])
    } else if (f0 == 0 && f1 == 1) {
      // same data as we have now
      array.clone
    } else {
      // inverse (complement) of what we have now
      val arr = array.clone
      val len = array.length
      var i = 0
      while (i < len) {
        arr(i) = (~arr(i)).asInstanceOf[Byte]
        i += 1
      }
      arr
    }
    BitArrayRasterData(arr, cols, rows)
  }

  override def mapDouble(f: Double => Double) = map(z => d2i(f(i2d(z))))
}

object BitArrayRasterData {
  def ofDim(cols: Int, rows: Int) = new BitArrayRasterData(Array.ofDim[Byte](((cols * rows) + 7) / 8), cols, rows)
  def empty(cols: Int, rows: Int) = ofDim(cols, rows)
}

