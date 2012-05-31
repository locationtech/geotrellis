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

  def combine2(other:RasterData)(f:(Int,Int) => Int) = {
    val output = other.copy
    var i = 0
    val len = length
    while (i < len) {
      output(i) = f(this(i), output(i))
      i += 1
    }
    output
  }
}

// TODO: move update to only exist in StrictRasterData
trait StrictRasterData extends RasterData with Serializable {
  override def copy():StrictRasterData
}

trait LazyRasterData extends RasterData {
  def update(i:Int, x:Int) = sys.error("immutable")

  override def equals(other:Any):Boolean = sys.error("todo")
}

// strict implementations follow

/**
 * RasterData based on Array[Int] (each cell as an Int).
 */
object IntArrayRasterData {
  def apply(array:Array[Int]) = new IntArrayRasterData(array)
  def ofDim(size:Int) = new IntArrayRasterData(Array.ofDim[Int](size))
  def empty(size:Int) = new IntArrayRasterData(Array.fill[Int](size)(NODATA))
}

final class IntArrayRasterData(array:Array[Int]) extends StrictRasterData {
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, x: Int): Unit = array(i) = x
  def copy = IntArrayRasterData(this.array.clone)
  def asArray = array
}

/**
 * RasterData based on an Array[Byte] as a bitmask; values are 0 and 1.
 */
object BitArrayRasterData {
  def apply(array:Array[Byte], size:Int) = new BitArrayRasterData(array, size)
  def ofDim(size:Int) = new BitArrayRasterData(Array.ofDim[Byte]((size + 7) / 8), size)
  def empty(size:Int) = ofDim(size)
}

final class BitArrayRasterData(array:Array[Byte], size:Int) extends StrictRasterData {
  assert(array.length == (size + 7) / 8)
  def length = size
  def apply(i:Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]
  def update(i:Int, x:Int): Unit = array(i >> 3) = ((x & 1) << (i & 7)).asInstanceOf[Byte]
  def copy = BitArrayRasterData(array.clone, size)
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
    BitArrayRasterData(arr, size)
  }

  override def mapIfSet(f:Int => Int) = map(f)
}

/**
 * RasterData based on Array[Byte] (each cell as a Byte).
 */
object ByteArrayRasterData {
  def apply(array:Array[Byte]) = new ByteArrayRasterData(array)
  def ofDim(size:Int) = new ByteArrayRasterData(Array.ofDim[Byte](size))
  def empty(size:Int) = new ByteArrayRasterData(Array.fill[Byte](size)(Byte.MinValue))
}

final class ByteArrayRasterData(array:Array[Byte]) extends StrictRasterData {
  def length = array.length
  def apply(i:Int) = array(i).asInstanceOf[Int]
  def update(i:Int, x: Int): Unit = array(i) = x.asInstanceOf[Byte]
  def copy = ByteArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])
}

/**
 * RasterData based on Array[Short] (each cell as a Short).
 */
object ShortArrayRasterData {
  def apply(array:Array[Short]) = new ShortArrayRasterData(array)
  def ofDim(size:Int) = new ShortArrayRasterData(Array.ofDim[Short](size))
  def empty(size:Int) = new ShortArrayRasterData(Array.fill[Short](size)(Short.MinValue))
}

final class ShortArrayRasterData(array:Array[Short]) extends StrictRasterData {
  def length = array.length
  def apply(i:Int) = array(i).asInstanceOf[Int]
  def update(i:Int, x: Int): Unit = array(i) = x.asInstanceOf[Short]
  def copy = ShortArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])
}


// lazy implementations follow

/**
 * This class is a lazy wrapper for any RasterData. It's only function is to
 * defer functions like map/mapIfSet/combine2 to produce other lazy instances.
 */
final class LazyWrapper(data:RasterData) extends LazyRasterData {
  def length = data.length
  def apply(i:Int) = data(i)
  def copy = this
  def asArray = data.asArray

  override def foreach(f:Int => Unit) = data.foreach(f)
  override def map(f:Int => Int) = LazyMap(data)(f)
  override def mapIfSet(f:Int => Int) = LazyMapIfSet(data)(f)(NODATA)
  override def combine2(other:RasterData)(f:(Int, Int) => Int) = LazyCombine2(data, other)(f)
}

object LazyWrapper {
  def apply(data:RasterData) = data match {
    case _:LazyRasterData => data
    case _ => new LazyWrapper(data)
  }
}

final class LazyMap(data:RasterData)(g:Int => Int) extends LazyRasterData {
  def length = data.length
  def apply(i:Int) = g(data(i))
  def copy = this
  def asArray = data.asArray.map(g)

  override def foreach(f:Int => Unit) = data.foreach(z => f(g(z)))
  override def map(f:Int => Int) = LazyMap(data)(z => f(g(z)))
  override def mapIfSet(f:Int => Int) = LazyMapIfSet(data)(z => f(g(z)))(g(NODATA))
  override def combine2(other:RasterData)(f:(Int, Int) => Int) = {
    LazyCombine2(data, other)((z1, z2) => f(g(z1), z2))
  }
}

object LazyMap {
  def apply(data:RasterData)(g:Int => Int) = new LazyMap(data)(g)
}

final class LazyMapIfSet(data:RasterData)(g:Int => Int)(g0:Int) extends LazyRasterData {
  def composed(z:Int) = if (z == NODATA) g0 else g(z)

  def length = data.length
  def apply(i:Int) = composed(data(i))
  def copy = this
  def asArray = data.asArray.map(composed)

  override def foreach(f:Int => Unit) = data.foreach(z => f(composed(z)))
  override def map(f:Int => Int) = LazyMapIfSet(data)(z => f(g(z)))(f(g0))
  override def mapIfSet(f:Int => Int) = LazyMapIfSet(data)(z => f(g(z)))(g0)
  override def combine2(other:RasterData)(f:(Int, Int) => Int) = {
    LazyCombine2(data, other)((z1, z2) => f(composed(z1), z2))
  }
}

object LazyMapIfSet {
  def apply(data:RasterData)(f:Int => Int)(f0:Int) = new LazyMapIfSet(data)(f)(f0)
}

final class LazyCombine2(data1:RasterData, data2:RasterData)(g:(Int, Int) => Int) extends LazyRasterData {
  def length = data1.length
  def apply(i:Int) = g(data1(i), data2(i))
  def copy = this
  def asArray = {
    val len = length
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = g(data1(i), data2(i))
      i += 1
    }
    arr
  }
  override def foreach(f:Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      val z1 = data1(i)
      val z2 = data2(i)
      f(g(z1, z2))
      i += 1
    }
  }
  override def map(f:Int => Int) = {
    LazyCombine2(data1, data2)((a, b) => f(g(a, b)))
  }
  override def mapIfSet(f:Int => Int) = {
    LazyCombine2(data1, data2)((a, b) => { val z = g(a, b); if (z != NODATA) f(z) else NODATA })
  }
  override def combine2(other:RasterData)(f:(Int, Int) => Int) = {
    // TODO: if we had LazyCombine3, 4, etc. we could do better
    LazyCombine2(this, other)(f)
  }
}

object LazyCombine2 {
  def apply(data1:RasterData, data2:RasterData)(g:(Int, Int) => Int) = {
    new LazyCombine2(data1, data2)(g)
  }
}


