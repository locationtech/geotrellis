package trellis

import sys.error

/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop in replacement for Array.
 *
 * Currently only Int, instead of generic.
 */
trait RasterData {
  def apply(i: Int): Int
  def copy():RasterData
  def length:Int
  def update(i:Int, x: Int)
  def asArray:Array[Int]
  def asList = asArray.toList

  override def toString = "RasterData(<%d values>)" format length

  override def equals(other:Any):Boolean = other match {
    case r:RasterData => {
      if (r == null) return false
      if (length != r.length) return false
      var i = 0
      val len = length
      while (i < len) {
        if (apply(i) != r(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }
}

class ArrayRasterData(array:Array[Int]) extends RasterData with Serializable {
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, x: Int): Unit = array(i) = x
  def copy = ArrayRasterData(this.array.clone)
  def asArray = array
}

object ArrayRasterData {
  def apply(array:Array[Int]) = new ArrayRasterData(array)
}
