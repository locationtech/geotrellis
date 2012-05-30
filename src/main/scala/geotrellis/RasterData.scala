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
  override def equals(other:Any):Boolean = other match {
    case r:RasterData => {
      if (r == null) return false
      val len = length
      if (len != r.length) return false
      var i = 0
      while (i < len) {
        if (apply(i) != r(i)) return false
        i += 1
      }
      true
    }
    case _ => false
  }

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

/**
 * RasterData based on Array[Int] (each cell as an Int).
 */
object ArrayRasterData {
  def apply(array:Array[Int]) = new ArrayRasterData(array)
}

final class ArrayRasterData(array:Array[Int]) extends RasterData with Serializable {
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, x: Int): Unit = array(i) = x
  def copy = ArrayRasterData(this.array.clone)
  def asArray = array
}

/**
 * RasterData based on an Array[Byte] as a bitmask; values are 0 and 1.
 */
object BitArrayRasterData {
  def apply(array:Array[Byte], size:Int) = new BitArrayRasterData(array, size)
}

final class BitArrayRasterData(array:Array[Byte], size:Int) extends RasterData with Serializable {
  assert(array.length == (size + 7) / 8)
  def length = size
  def apply(i:Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]
  def update(i:Int, x:Int): Unit = array(i >> 3) = ((x & 1) << (i & 7)).asInstanceOf[Byte]
  def copy = new BitArrayRasterData(array.clone, size)
  def asArray = {
    val len = size
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = apply(i)
      i += 1
    }
    arr
  }

  override def map(f:Int => Int) = {
    val f0 = f(0) & 1
    val f1 = f(1) & 1
    val arr = if (f0 == 0 && f1 == 0) {
      Array.ofDim[Byte](array.length)
    } else if (f0 == 1 && f1 == 1) {
      Array.fill[Byte](array.length)(7.asInstanceOf[Byte])
    } else if (f0 == 0 && f1 == 1) {
      array.clone
    } else {
      val arr = array.clone
      val len = array.length
      var i = 0
      while (i < len) {
        arr(i) = (~arr(i)).asInstanceOf[Byte]
        i += 1
      }
      arr
    }
    new BitArrayRasterData(arr, size)
  }

  override def mapIfSet(f:Int => Int) = map(f)
}

/**
 * RasterData based on Array[Byte] (each cell as a Byte).
 */
object ByteArrayRasterData {
  def apply(array:Array[Byte]) = new ByteArrayRasterData(array)
}

class ByteArrayRasterData(array:Array[Byte]) extends RasterData with Serializable {
  def length = array.length
  def apply(i:Int) = array(i).asInstanceOf[Int]
  def update(i:Int, x: Int): Unit = array(i) = x.asInstanceOf[Byte]
  def copy = new ByteArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])
}

/**
 * RasterData based on Array[Short] (each cell as a Short).
 */
object ShortArrayRasterData {
  def apply(array:Array[Short]) = new ShortArrayRasterData(array)
}

class ShortArrayRasterData(array:Array[Short]) extends RasterData with Serializable {
  def length = array.length
  def apply(i:Int) = array(i).asInstanceOf[Int]
  def update(i:Int, x: Int): Unit = array(i) = x.asInstanceOf[Short]
  def copy = new ShortArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])
}
