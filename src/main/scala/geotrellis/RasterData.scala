package geotrellis

import geotrellis.raster._

object RasterUtil {
  final val byteNodata = Byte.MinValue
  final val shortNodata = Short.MinValue

  // byte
  @inline final def b2i(n:Byte):Int = if (n == byteNodata) NODATA else n.toInt
  @inline final def i2b(n:Int):Byte = if (n == NODATA) byteNodata else n.toByte

  // short
  @inline final def s2i(n:Short):Int = if (n == shortNodata) NODATA else n.toInt
  @inline final def i2s(n:Int):Short = if (n == NODATA) shortNodata else n.toShort

  // int
  @inline final def i2f(n:Int):Float = if (n == NODATA) Float.NaN else n.toFloat
  @inline final def f2i(n:Float):Int = if (java.lang.Float.isNaN(n)) NODATA else n.toInt

  // double
  @inline final def i2d(n:Int):Double = if (n == NODATA) Double.NaN else n.toDouble
  @inline final def d2i(n:Double):Int = if (java.lang.Double.isNaN(n)) NODATA else n.toInt
}

import RasterUtil._

// helper traits
trait IntBasedArray {
  def apply(i:Int):Int
  def update(i:Int, z:Int):Unit

  def applyDouble(i:Int):Double = i2d(apply(i))
  def updateDouble(i:Int, z:Double):Unit = update(i, d2i(z))
}

trait DoubleBasedArray {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}


/**
 *
 */

object RasterData {
  def largestType(lhs:RasterData, rhs:RasterData) = {
    lhs.getType.union(rhs.getType)
  }
  def largestByType(lhs:RasterData, rhs:RasterData) = {
    if (largestType(lhs, rhs) == lhs.getType) lhs else rhs
  }
  def largestAlloc(lhs:RasterData, rhs:RasterData, size:Int) = {
    largestByType(lhs, rhs).alloc(size)
  }

  def allocByType(t:RasterType, size:Int):MutableRasterData = t match {
    case TypeBit => BitArrayRasterData.ofDim(size)
    case TypeByte => ByteArrayRasterData.ofDim(size)
    case TypeShort => ShortArrayRasterData.ofDim(size)
    case TypeInt => IntArrayRasterData.ofDim(size)
    case TypeFloat => FloatArrayRasterData.ofDim(size)
    case TypeDouble => DoubleArrayRasterData.ofDim(size)
  }

  def emptyByType(t:RasterType, size:Int):MutableRasterData = t match {
    case TypeBit => BitArrayRasterData.empty(size)
    case TypeByte => ByteArrayRasterData.empty(size)
    case TypeShort => ShortArrayRasterData.empty(size)
    case TypeInt => IntArrayRasterData.empty(size)
    case TypeFloat => FloatArrayRasterData.empty(size)
    case TypeDouble => DoubleArrayRasterData.empty(size)
  }
}

/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait RasterData {
  def getType: RasterType
  def alloc(size:Int): MutableRasterData

  def copy:RasterData
  def length:Int
  def lengthLong:Long 
  def convert(typ:RasterType):RasterData

  //TODO: RasterData should be lazy by default?
  def combine2(other:RasterData)(f:(Int,Int) => Int):RasterData
  def foreach(f: Int => Unit):Unit
  def map(f:Int => Int):RasterData
  def mapIfSet(f:Int => Int):RasterData

  def combineDouble2(other:RasterData)(f:(Double,Double) => Double):RasterData
  def foreachDouble(f: Double => Unit):Unit
  def mapDouble(f:Double => Double):RasterData
  def mapIfSetDouble(f:Double => Double):RasterData

  // TODO: these should probably be removed
  def fold[A](a: =>A)(f:(A, Int) => A):A = {
    var aa = a
    foreach(z => aa = f(aa, z))
    aa
  }
  def foldDouble[A](a: =>A)(f:(A, Double) => A):A = {
    var aa = a
    foreach(z => aa = f(aa, z))
    aa
  }

  /**
   * Return the current RasterData as an array.
   */
  def asArray:Option[ArrayRasterData]

  /**
   * Return the current RasterData values as a strict (calculated) ArrayRasterData.
   *
   * If your RasterData cannot be represented as an array, bad things will happen.
   * If your RasterData is lazy, any deferred calculations will be executed.
   */
  def force:Option[StrictRasterData]

  /**
   * Return a mutable version of the current raster.
   */
  def mutable:Option[MutableRasterData]

  def get(x:Int, y:Int, cols:Int):Int
  def getDouble(x:Int, y:Int, cols:Int):Double
}

/**
 * ArrayRasterData provides array-like access to the grid data of a raster.
 *
 * While often backed with a real Array for speed, it cannot be used for rasters
 * with more cells than the maximum size of a java array: 2.14 billion.
 */ 
trait ArrayRasterData extends RasterData {

  def asArray = Some(this)

  def convert(typ:RasterType):ArrayRasterData = LazyConvert(this, typ)

  def lengthLong = length

  override def equals(other:Any):Boolean = other match {
    case r:ArrayRasterData => {
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

  def apply(i: Int):Int
  def applyDouble(i:Int):Double

  def get(col:Int, row:Int, cols:Int) = apply(row * cols + col)
  def getDouble(col:Int, row:Int, cols:Int) = applyDouble(row * cols + col)

  def toList = toArray.toList
  def toListDouble = toArrayDouble.toList

  def toArray:Array[Int] = {
    val len = length
    val arr = Array.ofDim[Int](len)
    var i = 0
    while (i < len) {
      arr(i) = apply(i)
      i += 1
    }
    arr
  }

  def toArrayDouble:Array[Double] = {
    val len = length
    val arr = Array.ofDim[Double](len)
    var i = 0
    while (i < len) {
      arr(i) = applyDouble(i)
      i += 1
    }
    arr
  }
}


trait StrictRasterData extends ArrayRasterData with Serializable {
  def force = Some(this)

  def foreach(f:Int => Unit):Unit = {
    var i = 0
    val len = length
    while(i < len) {
      f(apply(i))
      i += 1
    }
  }

  def map(f:Int => Int):ArrayRasterData = {
    val len = length
    val data = alloc(len)
    var i = 0
    while (i < len) {
      data(i) = f(apply(i))
      i += 1
    }
    data
  }

  def mapIfSet(f:Int => Int):ArrayRasterData = {
    val len = length
    val data = alloc(len)
    var i = 0
    while (i < len) {
      val z = apply(i)
      if (z != NODATA) data(i) = f(z)
      i += 1
    }
    data
  }

  def combine2(rhs:RasterData)(f:(Int,Int) => Int):RasterData = rhs match {
    case other:ArrayRasterData => {
      val output = RasterData.largestAlloc(this, other, length)
      var i = 0
      val len = length
      while (i < len) {
        output(i) = f(apply(i), other(i))
        i += 1
      }
      output
    }
    case _ => rhs.combine2(this)((b, a) => f(a, b))
  }

  def foreachDouble(f:Double => Unit):Unit = {
    var i = 0
    val len = length
    while(i < len) {
      f(applyDouble(i))
      i += 1
    }
  }


  def mapDouble(f:Double => Double):ArrayRasterData = {
    val len = length
    val data = alloc(len)
    var i = 0
    while (i < len) {
      data.updateDouble(i, f(applyDouble(i)))
      i += 1
    }
    data
  }

  def mapIfSetDouble(f:Double => Double):ArrayRasterData = {
    val len = length
    val data = alloc(len)
    var i = 0
    while (i < len) {
      val z = applyDouble(i)
      if (java.lang.Double.isNaN(z)) data.updateDouble(i, f(z))
      i += 1
    }
    data
  }

  def combineDouble2(rhs:RasterData)(f:(Double,Double) => Double) = rhs match {
    case other:ArrayRasterData => {
      val output = RasterData.largestAlloc(this, other, length)
      var i = 0
      val len = length
      while (i < len) {
        output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
        i += 1
      }
      output
    }
    case _ => rhs.combineDouble2(this)((b, a) => f(a, b))
  }
}


trait MutableRasterData extends StrictRasterData {
  def mutable = Some(this)

  def update(i:Int, z:Int): Unit
  def updateDouble(i:Int, z:Double):Unit

  def set(col:Int, row:Int, value:Int, cols:Int) {
    update(row * cols + col, value)
  }
  def setDouble(col:Int, row:Int, value:Double, cols:Int) {
    updateDouble(row * cols + col, value)
  }
}


// TODO: also extend serializable?
trait LazyRasterData extends ArrayRasterData {
  def force = mutable
  def mutable = {
    val len = length
    val d = alloc(len)
    var i = 0
    while (i < len) {
      d(i) = apply(i)
      i += 1
    }
    Some(d)
  }
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

final class IntArrayRasterData(array:Array[Int]) extends MutableRasterData with IntBasedArray {
  def getType = TypeInt
  def alloc(size:Int) = IntArrayRasterData.ofDim(size)
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, z:Int) { array(i) = z }
  def copy = IntArrayRasterData(array.clone)
  override def toArray = array.clone
}

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
object BitArrayRasterData {
  def apply(array:Array[Byte], size:Int) = new BitArrayRasterData(array, size)
  def ofDim(size:Int) = new BitArrayRasterData(Array.ofDim[Byte]((size + 7) / 8), size)
  def empty(size:Int) = ofDim(size)
}

final class BitArrayRasterData(array:Array[Byte], size:Int)
extends MutableRasterData with IntBasedArray {
  // i >> 3 is the same as i / 8 but faster
  // i & 7 is the same as i % 8 but faster
  // i & 1 is the same as i % 2 but faster
  // ~3 -> -4, that is 00000011 -> 11111100
  // 3 | 9 -> 11, that is 00000011 | 00001001 -> 00001011
  // 3 & 9 -> 1,  that is 00000011 & 00001001 -> 00000001
  // 3 ^ 9 -> 10, that is 00000011 ^ 00001001 -> 00001010
  assert(array.length == (size + 7) / 8)
  def getType = TypeBit
  def alloc(size:Int) = BitArrayRasterData.ofDim(size)
  def length = size
  def apply(i:Int) = ((array(i >> 3) >> (i & 7)) & 1).asInstanceOf[Int]
  def update(i:Int, z:Int): Unit = {
    val div = i >> 3 
    if ((z & 1) == 0) {
      // unset the nth bit
      array(div) = (array(div) & ~(1 << (i & 7))).toByte
    } else {
      // set the nth bit
      array(div) = (array(div) | (1 << (i & 7))).toByte
    }
  }
  def copy = BitArrayRasterData(array.clone, size)

  override def map(f:Int => Int) = {
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
    BitArrayRasterData(arr, size)
  }

  override def mapIfSet(f:Int => Int) = map(f)

  override def mapDouble(f:Double => Double) = map(z => d2i(f(i2d(z))))
  override def mapIfSetDouble(f:Double => Double) = mapDouble(f)
}

/**
 * RasterData based on Array[Byte] (each cell as a Byte).
 */
object ByteArrayRasterData {
  def apply(array:Array[Byte]) = new ByteArrayRasterData(array)
  def ofDim(size:Int) = new ByteArrayRasterData(Array.ofDim[Byte](size))
  def empty(size:Int) = new ByteArrayRasterData(Array.fill[Byte](size)(Byte.MinValue))
}

final class ByteArrayRasterData(array:Array[Byte])
extends MutableRasterData with IntBasedArray {
  def getType = TypeByte
  def alloc(size:Int) = ByteArrayRasterData.ofDim(size)
  def length = array.length
  def apply(i:Int) = b2i(array(i))
  def update(i:Int, z:Int) { array(i) = i2b(z) }
  def copy = ByteArrayRasterData(array.clone)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != byteNodata) arr(i) = f(z).asInstanceOf[Byte]
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

final class ShortArrayRasterData(array:Array[Short])
extends MutableRasterData with IntBasedArray {
  def getType = TypeShort
  def alloc(size:Int) = ShortArrayRasterData.ofDim(size)
  def length = array.length
  def apply(i:Int) = s2i(array(i))
  def update(i:Int, z:Int) { array(i) = i2s(z) }
  def copy = ShortArrayRasterData(array.clone)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != shortNodata) arr(i) = f(z).asInstanceOf[Short]
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

final class FloatArrayRasterData(array:Array[Float])
extends MutableRasterData with DoubleBasedArray {
  def getType = TypeFloat
  def alloc(size:Int) = FloatArrayRasterData.ofDim(size)
  def length = array.length
  def applyDouble(i:Int) = array(i).toDouble
  def updateDouble(i:Int, z:Double) = array(i) = z.toFloat
  def copy = FloatArrayRasterData(array.clone)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Float.isNaN(z)) arr(i) = i2f(f(f2i(z)))
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

final class DoubleArrayRasterData(array:Array[Double])
extends MutableRasterData with DoubleBasedArray {
  def getType = TypeDouble
  def alloc(size:Int) = DoubleArrayRasterData.ofDim(size)
  def length = array.length
  def applyDouble(i:Int) = array(i)
  def updateDouble(i:Int, z:Double) = array(i) = z
  def copy = DoubleArrayRasterData(array.clone)
  override def toArrayDouble = array.clone

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Double.isNaN(z)) arr(i) = i2d(f(d2i(z)))
      i += 1
    }
    DoubleArrayRasterData(arr)
  }

}


// lazy implementations follow
trait Wrapper {
  protected[this] def underlying:ArrayRasterData
  final def getType = underlying.getType
  final def alloc(size:Int) = underlying.alloc(size)
  final def length = underlying.length
}


//REVIEW: create abstract Lazy classes for non-array implementations to share?
/**
 * This class is a lazy wrapper for any RasterData. It's only function is to
 * defer functions like map/mapIfSet/combine2 to produce other lazy instances.
 */
final class LazyArrayWrapper(data:ArrayRasterData)
extends LazyRasterData with Wrapper {
  def copy = this
  def underlying = data
  override def toArray = data.toArray
  override def toArrayDouble = data.toArrayDouble

  final def apply(i:Int) = underlying.apply(i)
  final def applyDouble(i:Int) = underlying.applyDouble(i)

  def foreach(f:Int => Unit) = data.foreach(f)
  def map(f:Int => Int) = LazyMap(underlying, f)
  def mapIfSet(f:Int => Int) = LazyMapIfSet(underlying, f)
  def combine2(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine2(underlying, a, f)
    case o => o.combine2(underlying)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(f)
  def mapDouble(f:Double => Double) = LazyMapDouble(underlying, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(underlying, f)
  def combineDouble2(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombine2(underlying, a, (z1,z2) => d2i(f(z1,z2)))
    case o => o.combine2(underlying)((z2, z1) => d2i(f(z1, z2)))
  }
}

object LazyArrayWrapper {
  def apply(data:ArrayRasterData):LazyRasterData = data match {
    case d:LazyRasterData => d
    case d => new LazyArrayWrapper(d)
  }
}

final class LazyMap(data:ArrayRasterData, g:Int => Int)
extends LazyRasterData with Wrapper {

  def copy = this
  def underlying = data

  final def apply(i:Int) = g(data(i))
  final def applyDouble(i:Int) = i2d(g(data(i)))

  def foreach(f:Int => Unit) = data.foreach(z => f(g(z)))
  def map(f:Int => Int) = LazyMap(data, z => f(g(z)))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(underlying, f)
  def combine2(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine2(data, a, (z1, z2) => f(g(z1), z2))
    case o => o.combine2(data)((z2, z1) => f(g(z1), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(z => f(g(d2i(z))))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(i2d(g(d2i(z)))))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(underlying, f)
  def combineDouble2(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble2(data, a, (z1, z2) => f(i2d(g(d2i(z1))), z2))
    case o => o.combineDouble2(data)((z2, z1) => f(i2d(g(d2i(z1))), z2))
  }
}

object LazyMap {
  def apply(data:ArrayRasterData, g:Int => Int) = new LazyMap(data, g)
}

object LazyMapDouble {
  def apply(data:ArrayRasterData, g:Double => Double) = new LazyMap(data, z => d2i(g(i2d(z))))
}

/**
 * g is the function that we will call on all cells that have data, and g0 is
 * the value that we will map all NODATA cells to.
 */
final class LazyMapIfSet(data:ArrayRasterData, g:Int => Int) extends LazyRasterData with Wrapper {
  def underlying = data
  def copy = this

  def gIfSet(z:Int) = if (z == NODATA) NODATA else g(z)

  final def apply(i:Int) = gIfSet(data(i))
  final def applyDouble(i:Int) = i2d(gIfSet(data(i)))

  def foreach(f:Int => Unit) = data.foreach(z => f(gIfSet(z)))
  def map(f:Int => Int) = LazyMap(data, z => f(gIfSet(z)))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(data, z => f(g(z)))
  def combine2(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine2(data, a, (z1, z2) => f(gIfSet(z1), z2))
    case o => o.combine2(data)((z2, z1) => f(gIfSet(z1), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(z => f(i2d(gIfSet(d2i(z)))))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(i2d(gIfSet(d2i(z)))))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(data, z => f(i2d(g(d2i(z)))))
  def combineDouble2(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble2(data, a, (z1, z2) => f(i2d(gIfSet(d2i(z1))), z2))
    case o => o.combineDouble2(data)((z2, z1) => f(i2d(gIfSet(d2i(z1))), z2))
  }
}

object LazyMapIfSet {
  def apply(data:ArrayRasterData, f:Int => Int) = new LazyMapIfSet(data, f)
}

object LazyMapIfSetDouble {
  def apply(data:ArrayRasterData, g:Double => Double) = new LazyMapIfSet(data, z => d2i(g(i2d(z))))
}

final class LazyCombine2(data1:ArrayRasterData,
                         data2:ArrayRasterData, g:(Int, Int) => Int) extends LazyRasterData {

  def getType = RasterData.largestType(data1, data2)
  def alloc(size:Int) = RasterData.largestAlloc(data1, data2, size)
  def length = data1.length

  def apply(i:Int) = g(data1.apply(i), data2.apply(i))
  def applyDouble(i:Int) = i2d(g(data1.apply(i), data2.apply(i)))
  def copy = this

  def foreach(f:Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(g(data1(i), data2(i)))
      i += 1
    }
  }

  def map(f:Int => Int) = LazyCombine2(data1, data2, (a, b) => f(g(a, b)))

  def mapIfSet(f:Int => Int) = {
    def h(a:Int, b:Int) = {
      val z = g(a, b)
      if (z != NODATA) f(z) else NODATA
    }
    LazyCombine2(data1, data2, h)
  }

  def combine2(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine2(this, a, f)
    case o => o.combine2(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(g(data1(i), data2(i)))
      i += 1
    }
  }

  def mapDouble(f:Double => Double) = {
    LazyCombineDouble2(data1, data2, (a, b) => f(i2d(g(d2i(a), d2i(b)))))
  }

  def mapIfSetDouble(f:Double => Double) = {
    def h(a:Double, b:Double) = {
      val z = i2d(g(d2i(a), d2i(b)))
      if (z != NODATA) f(z) else Double.NaN
    }
    LazyCombineDouble2(data1, data2, h)
  }

  def combineDouble2(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble2(this, a, f)
    case o => o.combineDouble2(this)((z2, z1) => f(z1, z2))
  }
}

object LazyCombine2 {
  def apply(data1:ArrayRasterData, data2:ArrayRasterData, g:(Int, Int) => Int) = {
    new LazyCombine2(data1, data2, g)
  }
}

object LazyCombineDouble2 {
  def apply(data1:ArrayRasterData, data2:ArrayRasterData, g:(Double, Double) => Double) = {
    new LazyCombine2(data1, data2, (z1, z2) => d2i(g(i2d(z1), i2d(z2))))
  }
}

final case class LazyConvert(data:ArrayRasterData, typ:RasterType)
extends LazyRasterData {
  def getType = typ
  def alloc(size:Int) = RasterData.allocByType(typ, size)
  def length = data.length
  def apply(i:Int) = data.apply(i)
  def applyDouble(i:Int) = data.applyDouble(i)
  def copy = this
  override def toArray = data.toArray
  override def toArrayDouble = data.toArrayDouble

  def foreach(f:Int => Unit) = data.foreach(f)
  def map(f:Int => Int) = LazyMap(data, f)
  def mapIfSet(f:Int => Int) = LazyMapIfSet(data, f)
  def combine2(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine2(data, a, f)
    case o => o.combine2(data)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(f)
  def mapDouble(f:Double => Double) = LazyMapDouble(data, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(data, f)
  def combineDouble2(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombine2(data, a, (z1,z2) => d2i(f(z1,z2)))
    case o => o.combineDouble2(data)((z2, z1) => f(z1, z2))
  }
}
