package geotrellis

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

  // currently disabled.
  // TODO: fix tests or reenable
  //override def equals(other:Any):Boolean = other match {
  //  case r:RasterData => {
  //    if (r == null) return false
  //    val len = length
  //    if (len != r.length) return false
  //    var i = 0
  //    while (i < len) {
  //      if (apply(i) != r(i)) return false
  //      i += 1
  //    }
  //    true
  //  }
  //  case _ => false
  //}

  def foreach(f: Int => Unit):Unit = {
    var i = 0
    val len = length
    while(i < len) {
      f(apply(i))
      i += 1
    }
  }

  def map(f:Int => Int) = {
    val data = this.copy
    var i = 0
    val len = length
    while (i < len) {
      data(i) = f(data(i))
      i += 1
    }
    data
  }

  def mapIfSet(f:Int => Int) = {
    val data = this.copy
    var i = 0
    val len = length
    while (i < len) {
      val z = data(i)
      if (z != NODATA) data(i) = f(z)
      i += 1
    }
    data
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
