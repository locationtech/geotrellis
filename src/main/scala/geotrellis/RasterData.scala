package geotrellis

import geotrellis.raster._

/**
 * The RasterUtil object contains a bunch of final values and methods used for
 * no data checks and conversions. It's important to avoid using toInt and
 * toDouble when converting raster values, since these methods don't have
 * NODATA/Double.NaN conversion correctly.
 */
object RasterUtil {
  final val byteNodata = Byte.MinValue
  final val shortNodata = Short.MinValue

  @inline final def isNodata(n:Int) = n == NODATA
  @inline final def isNodata(n:Double) = java.lang.Double.isNaN(n)

  @inline final def isData(n:Int) = n != NODATA
  @inline final def isData(n:Double) = !java.lang.Double.isNaN(n)

  @inline final def b2i(n:Byte):Int = if (n == byteNodata) NODATA else n.toInt
  @inline final def i2b(n:Int):Byte = if (n == NODATA) byteNodata else n.toByte

  @inline final def s2i(n:Short):Int = if (n == shortNodata) NODATA else n.toInt
  @inline final def i2s(n:Int):Short = if (n == NODATA) shortNodata else n.toShort

  @inline final def i2f(n:Int):Float = if (n == NODATA) Float.NaN else n.toFloat
  @inline final def f2i(n:Float):Int = if (java.lang.Float.isNaN(n)) NODATA else n.toInt

  @inline final def i2d(n:Int):Double = if (n == NODATA) Double.NaN else n.toDouble
  @inline final def d2i(n:Double):Int = if (java.lang.Double.isNaN(n)) NODATA else n.toInt
}

import RasterUtil._

/**
 * This trait defines applyDouble/updateDouble in terms of apply/update.
 */
trait IntBasedArray {
  def apply(i:Int):Int
  def update(i:Int, z:Int):Unit

  def applyDouble(i:Int):Double = i2d(apply(i))
  def updateDouble(i:Int, z:Double):Unit = update(i, d2i(z))
}

/**
 * This trait defines apply/update in terms of applyDouble/updateDouble.
 */
trait DoubleBasedArray {
  def apply(i:Int):Int = d2i(applyDouble(i))
  def update(i:Int, z:Int):Unit = updateDouble(i, i2d(z))

  def applyDouble(i:Int):Double
  def updateDouble(i:Int, z:Double):Unit
}


/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait RasterData {
  def getType: RasterType
  def alloc(cols:Int, rows:Int): MutableRasterData
  def isFloat = getType.float

  /**
    * True if this RasterData is tiled, e.g. a subclass of TiledRasterData. 
    */
  def isTiled:Boolean = false

  def copy:RasterData
  def length:Int
  def lengthLong:Long 
  def convert(typ:RasterType):RasterData

  def cols:Int
  def rows:Int

  /**
   * Combine two RasterData's cells into new cells using the given integer
   * function. For every (x,y) cell coordinate, get each RasterData's integer
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combine(other:RasterData)(f:(Int, Int) => Int):RasterData

  /**
   * For every cell in the given raster, run the given integer function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreach(f:Int => Unit):Unit

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def map(f:Int => Int):RasterData

  /**
   * Similar to map, except that this method passes through "nodata" cells
   * without calling the provided function.
   */
  def mapIfSet(f:Int => Int):RasterData

  /**
   * Combine two RasterData's cells into new cells using the given double
   * function. For every (x,y) cell coordinate, get each RasterData's double
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combineDouble(other:RasterData)(f:(Double, Double) => Double):RasterData

  /**
   * For every cell in the given raster, run the given double function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreachDouble(f:Double => Unit):Unit

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def mapDouble(f:Double => Double):RasterData

  /**
   * Similar to map, except that this method passes through "nodata" cells
   * without calling the provided function.
   */
  def mapIfSetDouble(f:Double => Double):RasterData

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

  /**
   * Get a particular (x, y) cell's integer value.
   */
  def get(x:Int, y:Int):Int

  /**
   * Get a particular (x, y) cell's double value.
   */
  def getDouble(x:Int, y:Int):Double
}

object RasterData {
  def largestType(lhs:RasterData, rhs:RasterData) = {
    lhs.getType.union(rhs.getType)
  }
  def largestByType(lhs:RasterData, rhs:RasterData) = {
    if (largestType(lhs, rhs) == lhs.getType) lhs else rhs
  }
  def largestAlloc(lhs:RasterData, rhs:RasterData, cols:Int, rows:Int) = {
    largestByType(lhs, rhs).alloc(cols, rows)
  }

  def allocByType(t:RasterType, cols:Int, rows:Int):MutableRasterData = t match {
    case TypeBit => BitArrayRasterData.ofDim(cols, rows)
    case TypeByte => ByteArrayRasterData.ofDim(cols, rows)
    case TypeShort => ShortArrayRasterData.ofDim(cols, rows)
    case TypeInt => IntArrayRasterData.ofDim(cols, rows)
    case TypeFloat => FloatArrayRasterData.ofDim(cols, rows)
    case TypeDouble => DoubleArrayRasterData.ofDim(cols, rows)
  }

  def emptyByType(t:RasterType, cols:Int, rows:Int):MutableRasterData = t match {
    case TypeBit => BitArrayRasterData.empty(cols, rows)
    case TypeByte => ByteArrayRasterData.empty(cols, rows)
    case TypeShort => ShortArrayRasterData.empty(cols, rows)
    case TypeInt => IntArrayRasterData.empty(cols, rows)
    case TypeFloat => FloatArrayRasterData.empty(cols, rows)
    case TypeDouble => DoubleArrayRasterData.empty(cols, rows)
  }
}


/**
 * ArrayRasterData provides array-like access to the grid data of a raster.
 *
 * While often backed with a real Array for speed, it cannot be used for rasters
 * with more cells than the maximum size of a java array: 2.14 billion.
 */ 
trait ArrayRasterData extends RasterData {

  def asArray = Option(this)

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

  def get(col:Int, row:Int) = apply(row * cols + col)
  def getDouble(col:Int, row:Int) = applyDouble(row * cols + col)

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


/**
 * StrictRasterData is an ArrayRasterData which has already allocated its
 * values and which evaluates eagerly.
 *
 * This trait provides concrete, eager implementations of map, mapIfSet,
 * foreach, and combine.
 */
trait StrictRasterData extends ArrayRasterData with Serializable {
  def force = Option(this)

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
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      data(i) = f(apply(i))
      i += 1
    }
    data
  }

  def mapIfSet(f:Int => Int):ArrayRasterData = {
    val len = length
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      val z = apply(i)
      if (isData(z)) data(i) = f(z)
      i += 1
    }
    data
  }

  def combine(rhs:RasterData)(f:(Int, Int) => Int):RasterData = rhs match {
    case other:ArrayRasterData => {
      if (lengthLong != other.lengthLong) {
        val size1 = s"${cols} x ${rows}"
        val size2 = s"${other.cols} x ${other.rows}"
        sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
      }
      val output = RasterData.largestAlloc(this, other, cols, rows)
      var i = 0
      val len = length
      while (i < len) {
        output(i) = f(apply(i), other(i))
        i += 1
      }
      output
    }
    case _ => rhs.combine(this)((b, a) => f(a, b))
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
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      data.updateDouble(i, f(applyDouble(i)))
      i += 1
    }
    data
  }

  def mapIfSetDouble(f:Double => Double):ArrayRasterData = {
    val len = length
    val data = alloc(cols, rows)
    var i = 0
    while (i < len) {
      val z = applyDouble(i)
      if (isData(z)) data.updateDouble(i, f(z))
      i += 1
    }
    data
  }

  def combineDouble(rhs:RasterData)(f:(Double, Double) => Double) = rhs match {
    case other:ArrayRasterData => {
      if (lengthLong != other.lengthLong) {
        val size1 = s"${cols} x ${rows}"
        val size2 = s"${other.cols} x ${other.rows}"
        sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
      }
      val output = RasterData.largestAlloc(this, other, cols, rows)
      var i = 0
      val len = length
      while (i < len) {
        output.updateDouble(i, f(applyDouble(i), other.applyDouble(i)))
        i += 1
      }
      output
    }
    case _ => rhs.combineDouble(this)((b, a) => f(a, b))
  }
}


/**
 * MutableRasterData is a StrictRasterData whose cells can be written to
 * (mutated).
 */
trait MutableRasterData extends StrictRasterData {
  def mutable = Option(this)

  def update(i:Int, z:Int): Unit
  def updateDouble(i:Int, z:Double):Unit

  def set(col:Int, row:Int, value:Int) {
    update(row * cols + col, value)
  }
  def setDouble(col:Int, row:Int, value:Double) {
    updateDouble(row * cols + col, value)
  }
}


/**
 * RasterData based on Array[Int] (each cell as an Int).
 */
final case class IntArrayRasterData(array:Array[Int], cols:Int, rows:Int) extends MutableRasterData with IntBasedArray {
  def getType = TypeInt
  def alloc(cols:Int, rows:Int) = IntArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i:Int) = array(i)
  def update(i:Int, z:Int) { array(i) = z }
  def copy = IntArrayRasterData(array.clone, cols, rows)
  override def toArray = array.clone
}

object IntArrayRasterData {
  //def apply(array:Array[Int]) = new IntArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new IntArrayRasterData(Array.ofDim[Int](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new IntArrayRasterData(Array.fill[Int](cols * rows)(NODATA), cols, rows)
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
final case class BitArrayRasterData(array:Array[Byte], cols:Int, rows:Int)
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
  def alloc(cols:Int, rows:Int) = BitArrayRasterData.ofDim(cols, rows)
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
  def copy = BitArrayRasterData(array.clone, cols, rows)

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
    BitArrayRasterData(arr, cols, rows)
  }

  override def mapIfSet(f:Int => Int) = map(f)

  override def mapDouble(f:Double => Double) = map(z => d2i(f(i2d(z))))
  override def mapIfSetDouble(f:Double => Double) = mapDouble(f)
}

object BitArrayRasterData {
  def ofDim(cols:Int, rows:Int) = new BitArrayRasterData(Array.ofDim[Byte](((cols * rows) + 7) / 8), cols, rows)
  def empty(cols:Int, rows:Int) = ofDim(cols, rows)
}


/**
 * RasterData based on Array[Byte] (each cell as a Byte).
 */
final case class ByteArrayRasterData(array:Array[Byte], cols:Int, rows:Int)
extends MutableRasterData with IntBasedArray {
  def getType = TypeByte
  def alloc(cols:Int, rows:Int) = ByteArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i:Int) = b2i(array(i))
  def update(i:Int, z:Int) { array(i) = i2b(z) }
  def copy = ByteArrayRasterData(array.clone, cols, rows)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != byteNodata) arr(i) = f(z).asInstanceOf[Byte]
      i += 1
    }
    ByteArrayRasterData(arr, cols, rows)
  }
}

object ByteArrayRasterData {
  //def apply(array:Array[Byte]) = new ByteArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new ByteArrayRasterData(Array.ofDim[Byte](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new ByteArrayRasterData(Array.fill[Byte](cols * rows)(Byte.MinValue), cols, rows)
}


/**
 * RasterData based on Array[Short] (each cell as a Short).
 */
final case class ShortArrayRasterData(array:Array[Short], cols:Int, rows:Int)
extends MutableRasterData with IntBasedArray {
  def getType = TypeShort
  def alloc(cols:Int, rows:Int) = ShortArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def apply(i:Int) = s2i(array(i))
  def update(i:Int, z:Int) { array(i) = i2s(z) }
  def copy = ShortArrayRasterData(array.clone, cols, rows)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (z != shortNodata) arr(i) = f(z).asInstanceOf[Short]
      i += 1
    }
    ShortArrayRasterData(arr, cols, rows)
  }
}

object ShortArrayRasterData {
  //def apply(array:Array[Short]) = new ShortArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new ShortArrayRasterData(Array.ofDim[Short](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new ShortArrayRasterData(Array.fill[Short](cols * rows)(Short.MinValue), cols, rows)
}


/**
 * RasterData based on Array[Float] (each cell as a Float).
 */
final case class FloatArrayRasterData(array:Array[Float], cols:Int, rows:Int)
extends MutableRasterData with DoubleBasedArray {
  def getType = TypeFloat
  def alloc(cols:Int, rows:Int) = FloatArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i:Int) = array(i).toDouble
  def updateDouble(i:Int, z:Double) = array(i) = z.toFloat
  def copy = FloatArrayRasterData(array.clone, cols, rows)

  override def mapIfSet(f:Int => Int) = {
    val arr = array.clone
    var i = 0
    val len = length
    while (i < len) {
      val z = arr(i)
      if (!java.lang.Float.isNaN(z)) arr(i) = i2f(f(f2i(z)))
      i += 1
    }
    FloatArrayRasterData(arr, cols, rows)
  }
}

object FloatArrayRasterData {
  //def apply(array:Array[Float]) = new FloatArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new FloatArrayRasterData(Array.ofDim[Float](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new FloatArrayRasterData(Array.fill[Float](cols * rows)(Float.NaN), cols, rows)
}


/**
 * RasterData based on Array[Double] (each cell as a Double).
 */
final case class DoubleArrayRasterData(array:Array[Double], cols:Int, rows:Int)
extends MutableRasterData with DoubleBasedArray {
  def getType = TypeDouble
  def alloc(cols:Int, rows:Int) = DoubleArrayRasterData.ofDim(cols, rows)
  def length = array.length
  def applyDouble(i:Int) = array(i)
  def updateDouble(i:Int, z:Double) = array(i) = z
  def copy = DoubleArrayRasterData(array.clone, cols, rows)
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
    DoubleArrayRasterData(arr, cols, rows)
  }
}

object DoubleArrayRasterData {
  //def apply(array:Array[Double]) = new DoubleArrayRasterData(array)
  def ofDim(cols:Int, rows:Int) = new DoubleArrayRasterData(Array.ofDim[Double](cols * rows), cols, rows)
  def empty(cols:Int, rows:Int) = new DoubleArrayRasterData(Array.fill[Double](cols * rows)(Double.NaN), cols, rows)
}


/**
 * LazyRasterData is an ArrayRasterData which (may) be lazily evaluated, and
 * which will perform other operations lazily.
 */
trait LazyRasterData extends ArrayRasterData {
  def force = mutable

  def mutable = {
    val len = length
    val d = alloc(cols, rows)
    var i = 0
    while (i < len) {
      d(i) = apply(i)
      i += 1
    }
    Option(d)
  }
}


/**
 * Wrapper is a mixin which implements some RasterData methods in terms of an
 * underlying raster data.
 */
trait Wrapper {
  protected[this] def underlying:ArrayRasterData
  final def getType = underlying.getType
  final def alloc(cols:Int, rows:Int) = underlying.alloc(cols, rows)
  final def length = underlying.length

  def cols = underlying.cols
  def rows = underlying.rows
}


/**
 * This class is a lazy wrapper for any RasterData. It's only function is to
 * defer functions like map/mapIfSet/combine to produce other lazy instances.
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
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(underlying, a, f)
    case o => o.combine(underlying)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(f)
  def mapDouble(f:Double => Double) = LazyMapDouble(underlying, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(underlying, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(underlying, a, f)
    case o => o.combineDouble(underlying)((z2, z1) => f(z1, z2))
  }
}

object LazyArrayWrapper {
  def apply(data:ArrayRasterData):LazyRasterData = data match {
    case d:LazyRasterData => d
    case d => new LazyArrayWrapper(d)
  }
}


/**
 * LazyMap represents a lazily-applied map method.
 */
final case class LazyMap(data:ArrayRasterData, g:Int => Int)
extends LazyRasterData with Wrapper {

  def copy = this
  def underlying = data

  final def apply(i:Int) = g(data(i))
  final def applyDouble(i:Int) = i2d(g(data(i)))

  def foreach(f:Int => Unit) = data.foreach(z => f(g(z)))
  def map(f:Int => Int) = LazyMap(data, z => f(g(z)))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(g(z1), z2))
    case o => o.combine(data)((z2, z1) => f(g(z1), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreach(z => f(i2d(g(z))))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(i2d(g(d2i(z)))))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(i2d(g(d2i(z1))), z2))
    case o => o.combineDouble(data)((z2, z1) => f(i2d(g(d2i(z1))), z2))
  }
}


/**
 * LazyMapDouble represents a lazily-applied mapDouble method.
 */
final case class LazyMapDouble(data:ArrayRasterData, g:Double => Double)
extends LazyRasterData with Wrapper {

  def copy = this
  def underlying = data

  final def apply(i:Int) = d2i(g(data.applyDouble(i)))
  final def applyDouble(i:Int) = g(data.applyDouble(i))

  def foreach(f:Int => Unit) = data.foreachDouble(z => f(d2i(g(z))))
  def map(f:Int => Int) = LazyMap(data, z => f(d2i(g(i2d(z)))))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(d2i(g(i2d(z1))), z2))
    case o => o.combine(data)((z2, z1) => f(d2i(g(i2d(z1))), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(z => f(g(z)))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(g(z)))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(g(z1), z2))
    case o => o.combineDouble(data)((z2, z1) => f(g(z1), z2))
  }
}


/**
 * LazyMapIfSet represents a lazily-applied mapIfSet method.
 */
final case class LazyMapIfSet(data:ArrayRasterData, g:Int => Int)
extends LazyRasterData with Wrapper {
  def underlying = data
  def copy = this

  def gIfSet(z:Int) = if (isNodata(z)) NODATA else g(z)

  final def apply(i:Int) = gIfSet(data(i))
  final def applyDouble(i:Int) = i2d(gIfSet(data(i)))

  def foreach(f:Int => Unit) = data.foreach(z => f(gIfSet(z)))
  def map(f:Int => Int) = LazyMap(data, z => f(gIfSet(z)))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(data, z => f(g(z)))
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(gIfSet(z1), z2))
    case o => o.combine(data)((z2, z1) => f(gIfSet(z1), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreach(z => f(i2d(gIfSet(z))))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(i2d(gIfSet(d2i(z)))))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(data, z => f(i2d(g(d2i(z)))))
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(i2d(gIfSet(d2i(z1))), z2))
    case o => o.combineDouble(data)((z2, z1) => f(i2d(gIfSet(d2i(z1))), z2))
  }
}


/**
 * LazyMapIfSetDouble represents a lazily-applied mapIfSet method.
 */
final case class LazyMapIfSetDouble(data:ArrayRasterData, g:Double => Double)
extends LazyRasterData with Wrapper {
  def underlying = data
  def copy = this

  def gIfSet(z:Double) = if (isNodata(z)) Double.NaN else g(z)

  final def apply(i:Int) = d2i(gIfSet(data(i)))
  final def applyDouble(i:Int) = gIfSet(data(i))

  def foreach(f:Int => Unit) = data.foreachDouble(z => f(d2i(gIfSet(z))))
  def map(f:Int => Int) = LazyMap(data, z => f(d2i(gIfSet(i2d(z)))))
  def mapIfSet(f:Int => Int) = LazyMapIfSet(data, z => f(d2i(g(i2d(z)))))
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(d2i(gIfSet(i2d(z1))), z2))
    case o => o.combine(data)((z2, z1) => f(d2i(gIfSet(i2d(z1))), z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(z => f(gIfSet(z)))
  def mapDouble(f:Double => Double) = LazyMapDouble(data, z => f(gIfSet(z)))
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(data, z => f(g(z)))
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(gIfSet(z1), z2))
    case o => o.combineDouble(data)((z2, z1) => f(gIfSet(z1), z2))
  }
}


/**
 * LazyCombine represents a lazily-applied combine method.
 */
final case class LazyCombine(data1:ArrayRasterData,
                             data2:ArrayRasterData,
                             g:(Int, Int) => Int) extends LazyRasterData {
  if (data1.lengthLong != data2.lengthLong) {
    val size1 = s"${data1.cols} x ${data1.rows}"
    val size2 = s"${data2.cols} x ${data2.rows}"
    sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
  }

  def cols = data1.cols
  def rows = data1.rows

  def getType = RasterData.largestType(data1, data2)
  def alloc(cols:Int, rows:Int) = RasterData.largestAlloc(data1, data2, cols, rows)
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

  def map(f:Int => Int) = LazyCombine(data1, data2, (a, b) => f(g(a, b)))

  def mapIfSet(f:Int => Int) = {
    def h(a:Int, b:Int) = {
      val z = g(a, b)
      if (isNodata(z)) NODATA else f(z)
    }
    LazyCombine(data1, data2, h)
  }

  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(i2d(g(data1(i), data2(i))))
      i += 1
    }
  }

  def mapDouble(f:Double => Double) = {
    LazyCombineDouble(data1, data2, (a, b) => f(i2d(g(d2i(a), d2i(b)))))
  }

  def mapIfSetDouble(f:Double => Double) = {
    def h(a:Double, b:Double) = {
      val z = g(d2i(a), d2i(b))
      if (isNodata(z)) Double.NaN else f(i2d(z))
    }
    LazyCombineDouble(data1, data2, h)
  }

  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}


/**
 * LazyCombineDouble represents a lazily-applied combineDouble method.
 */
final case class LazyCombineDouble(data1:ArrayRasterData,
                                   data2:ArrayRasterData,
                                   g:(Double, Double) => Double) extends LazyRasterData {

  if (data1.lengthLong != data2.lengthLong) {
    val size1 = s"${data1.cols} x ${data1.rows}"
    val size2 = s"${data2.cols} x ${data2.rows}"
    sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
  }

  def cols = data1.cols
  def rows = data1.rows

  def getType = RasterData.largestType(data1, data2)
  def alloc(cols:Int, rows:Int) = RasterData.largestAlloc(data1, data2, cols, rows)
  def length = data1.length

  def apply(i:Int) = d2i(g(data1.applyDouble(i), data2.applyDouble(i)))
  def applyDouble(i:Int) = g(data1.applyDouble(i), data2.applyDouble(i))
  def copy = this

  def foreach(f:Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(d2i(g(data1.applyDouble(i), data2.applyDouble(i))))
      i += 1
    }
  }

  def map(f:Int => Int) = LazyCombine(data1, data2, (a, b) => f(d2i(g(i2d(a), i2d(b)))))

  def mapIfSet(f:Int => Int) = {
    def h(a:Int, b:Int) = {
      val z = g(a, b)
      if (isNodata(z)) NODATA else f(d2i(z))
    }
    LazyCombine(data1, data2, h)
  }

  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(g(data1.applyDouble(i), data2.applyDouble(i)))
      i += 1
    }
  }

  def mapDouble(f:Double => Double) = {
    LazyCombineDouble(data1, data2, (a, b) => f(g(a, b)))
  }

  def mapIfSetDouble(f:Double => Double) = {
    def h(a:Double, b:Double) = {
      val z = g(a, b)
      if (isNodata(z)) Double.NaN else f(z)
    }
    LazyCombineDouble(data1, data2, h)
  }

  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}

/**
 * LazyConvertToBit represents a lazily-applied conversion from any type to TypeBit.
 *
 * @note     If you care converting to a RasterType with less bits
 *           than the type of the underlying data, you are responsible
 *           for managing overflow. This convert does not do any casting;
 *           therefore converting from a TypeInt to TypeByte could still
 *           return values greater than 127 from apply(). 
 */
final case class LazyConvert(data:ArrayRasterData, typ:RasterType)
extends LazyRasterData {

  def cols = data.cols
  def rows = data.rows

  def getType = typ
  def alloc(cols:Int, rows:Int) = RasterData.allocByType(typ, cols, rows)
  def length = data.length
  def apply(i:Int) = data.apply(i)
  def applyDouble(i:Int) = data.applyDouble(i)
  def copy = this
  override def toArray = data.toArray
  override def toArrayDouble = data.toArrayDouble

  def foreach(f:Int => Unit) = data.foreach(f)
  def map(f:Int => Int) = LazyMap(this, f)
  def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) = data.foreachDouble(f)
  def mapDouble(f:Double => Double) = LazyMapDouble(this, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
