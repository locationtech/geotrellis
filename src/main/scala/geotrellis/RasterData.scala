package geotrellis

import sys.error

import geotrellis.raster._

/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait RasterData {
  def getType: RasterType

  def apply(i: Int):Int
  def copy():RasterData
  def length:Int
  def update(i:Int, x:Int)
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

  // alternate double-based implementations
  def applyDouble(i:Int):Double = apply(i).toDouble
  def updateDouble(i:Int, x:Double):Unit = update(i, x.toInt)
  def asArrayDouble:Array[Double] = asArray.map(_.toDouble)
  def asListDouble = asArrayDouble.toList

  def foreachDouble(f:Double => Unit):Unit = foreach(z => f(z))
  def mapDouble(f:Double => Double) = map(z => f(z).toInt)
  def mapIfSetDouble(f:Double => Double) = mapIfSet(z => f(z).toInt)
  def combineDouble2(other:RasterData)(f:(Double,Double) => Double) = {
    val output = other match {
      case _:FloatArrayRasterData => other.copy
      case _:DoubleArrayRasterData => other.copy
      case _ => this.copy
    }
    var i = 0
    val len = length
    while (i < len) {
      output.updateDouble(i, f(this.applyDouble(i), other.applyDouble(i)))
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
  def getType = TypeInt
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
  def getType = TypeBit
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
  final val nd = Byte.MinValue
  def getType = TypeByte
  def length = array.length
  def apply(i:Int) = {
    val z = array(i)
    if (z == nd) NODATA else z
  }
  def update(i:Int, x: Int) {
    val z = if (x == NODATA) nd else x.asInstanceOf[Byte]
    array(i) = z
  }
  def copy = ByteArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != nd) arr(i) = f(z).asInstanceOf[Byte]
      i += 1
    }
    ByteArrayRasterData(arr)
  }
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
  final val nd = Byte.MinValue
  def getType = TypeShort
  def length = array.length
  def apply(i:Int) = {
    val z = array(i)
    if (z == nd) NODATA else z
  }
  def update(i:Int, x: Int) {
    val z = if (x == NODATA) nd else x.asInstanceOf[Short]
    array(i) = z
  }
  def copy = ShortArrayRasterData(array.clone)
  def asArray = array.map(_.asInstanceOf[Int])

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != nd) arr(i) = f(z).asInstanceOf[Short]
      i += 1
    }
    ShortArrayRasterData(arr)
  }
}

// floating-point rasters

/**
 * RasterData based on Array[Float] (each cell as a Float).
 */
object FloatArrayRasterData {
  def apply(array:Array[Float]) = new FloatArrayRasterData(array)
  def ofDim(size:Int) = new FloatArrayRasterData(Array.ofDim[Float](size))
  def empty(size:Int) = new FloatArrayRasterData(Array.fill[Float](size)(Float.NaN))
}

final class FloatArrayRasterData(array:Array[Float]) extends StrictRasterData {
  def getType = TypeFloat
  def length = array.length
  def apply(i:Int) = {
    val z = array(i)
    if (java.lang.Float.isNaN(z)) NODATA else z.toInt
  }
  def update(i:Int, x:Int) {
    val z = if (x == NODATA) Float.NaN else x.toFloat
    array(i) = z
  }
  def copy = FloatArrayRasterData(array.clone)
  def asArray = {
    val arr = Array.ofDim[Int](length)
    var i = 0
    val len = length
    while (i < len) {
      arr(i) = array(i).toInt
      i += 1
    }
    arr
  }

  override def map(f:Int => Int) = {
    val arr = Array.ofDim[Int](length)
    var i = 0
    val len = length
    while (i < len) {
      arr(i) = f(array(i).toInt)
      i += 1
    }
    IntArrayRasterData(arr)
  }

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Float.isNaN(z)) arr(i) = f(z.toInt)
      i += 1
    }
    FloatArrayRasterData(arr)
  }

  override def applyDouble(i:Int) = array(i).toDouble
  override def updateDouble(i:Int, x:Double) = array(i) = x.toFloat
  override def asArrayDouble = array.map(_.toDouble)

  override def foreachDouble(f:Double => Unit):Unit = {
    var i = 0
    val len = length
    while(i < len) {
      f(array(i))
      i += 1
    }
  }

  override def mapDouble(f:Double => Double) = {
    val data = copy()
    var i = 0
    val len = length
    while (i < len) {
      data.updateDouble(i, f(data.applyDouble(i)))
      i += 1
    }
    data
  }

  override def mapIfSetDouble(f:Double => Double) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Float.isNaN(z)) arr(i) = f(z).toFloat
      i += 1
    }
    FloatArrayRasterData(arr)
  }
}


/**
 * RasterData based on Array[Double] (each cell as a Double).
 */
object DoubleArrayRasterData {
  def apply(array:Array[Double]) = new DoubleArrayRasterData(array)
  def ofDim(size:Int) = new DoubleArrayRasterData(Array.ofDim[Double](size))
  def empty(size:Int) = new DoubleArrayRasterData(Array.fill[Double](size)(Double.NaN))
}

final class DoubleArrayRasterData(array:Array[Double]) extends StrictRasterData {
  def getType = TypeDouble
  def length = array.length
  def apply(i:Int) = {
    val z = array(i)
    if (java.lang.Double.isNaN(z)) NODATA else z.toInt
  }
  def update(i:Int, x:Int) {
    val z = if (x == NODATA) Double.NaN else x.toDouble
    array(i) = z
  }
  def copy = DoubleArrayRasterData(array.clone)
  def asArray = {
    val arr = Array.ofDim[Int](length)
    var i = 0
    val len = length
    while (i < len) {
      arr(i) = array(i).toInt
      i += 1
    }
    arr
  }

  override def map(f:Int => Int) = {
    val arr = Array.ofDim[Int](length)
    var i = 0
    val len = length
    while (i < len) {
      arr(i) = f(array(i).toInt)
      i += 1
    }
    IntArrayRasterData(arr)
  }

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Double.isNaN(z)) arr(i) = f(z.toInt)
      i += 1
    }
    DoubleArrayRasterData(arr)
  }

  override def applyDouble(i:Int) = array(i)
  override def updateDouble(i:Int, x:Double) = array(i) = x
  override def asArrayDouble = array

  override def foreachDouble(f:Double => Unit):Unit = {
    var i = 0
    val len = length
    while(i < len) {
      f(array(i))
      i += 1
    }
  }

  override def mapDouble(f:Double => Double) = {
    val data = copy()
    var i = 0
    val len = length
    while (i < len) {
      data.updateDouble(i, f(data.applyDouble(i)))
      i += 1
    }
    data
  }

  override def mapIfSetDouble(f:Double => Double) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Double.isNaN(z)) arr(i) = f(z)
      i += 1
    }
    DoubleArrayRasterData(arr)
  }

  override def combineDouble2(other:RasterData)(f:(Double,Double) => Double) = {
    val output = this.copy
    var i = 0
    val len = length
    while (i < len) {
      output.updateDouble(i, f(applyDouble(i), output.applyDouble(i)))
      i += 1
    }
    output
  }
}


// lazy implementations follow

/**
 * This class is a lazy wrapper for any RasterData. It's only function is to
 * defer functions like map/mapIfSet/combine2 to produce other lazy instances.
 */
final class LazyWrapper(data:RasterData) extends LazyRasterData {
  def getType = data.getType
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
  def getType = data.getType
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

  def getType = data.getType
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
  def getType = data1.getType
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
