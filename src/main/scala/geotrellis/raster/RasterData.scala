package geotrellis.raster

import geotrellis._

/**
 * RasterData provides access and update to the grid data of a raster.
 *
 * Designed to be a near drop-in replacement for Array in many cases.
 */
trait RasterData extends Serializable {
  def force:RasterData
  def getType: RasterType
  def alloc(cols: Int, rows: Int): MutableRasterData

  def isFloat = getType.float
  def convert(typ:RasterType):RasterData = LazyConvert(this, typ)
  def lengthLong = length

  def isLazy: Boolean = false

  def copy: RasterData
  def length: Int

  def cols: Int
  def rows: Int

  /**
   * Combine two RasterData's cells into new cells using the given integer
   * function. For every (x,y) cell coordinate, get each RasterData's integer
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combine(other: RasterData)(f: (Int, Int) => Int): RasterData

  /**
   * For every cell in the given raster, run the given integer function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreach(f: Int => Unit): Unit

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def map(f: Int => Int): RasterData

  /**
   * Combine two RasterData's cells into new cells using the given double
   * function. For every (x,y) cell coordinate, get each RasterData's double
   * value, map them to a new value, and assign it to the output's (x,y) cell.
   */
  def combineDouble(other: RasterData)(f: (Double, Double) => Double): RasterData

  /**
   * For every cell in the given raster, run the given double function.
   *
   * The order of the traversal from the lowest to highest columns, across each
   * row, but this should probably not be relied upon. In the future we'd like
   * to be able to parallelize foreach.
   */
  def foreachDouble(f: Double => Unit): Unit

  /**
   * Map each cell in the given raster to a new one, using the given function.
   */
  def mapDouble(f: Double => Double): RasterData

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

object RasterData {
  def largestType(lhs: RasterData, rhs: RasterData) = {
    lhs.getType.union(rhs.getType)
  }
  def largestByType(lhs: RasterData, rhs: RasterData) = {
    if (largestType(lhs, rhs) == lhs.getType) lhs else rhs
  }
  def largestAlloc(lhs: RasterData, rhs: RasterData, cols: Int, rows: Int) = {
    largestByType(lhs, rhs).alloc(cols, rows)
  }

  def allocByType(t: RasterType, cols: Int, rows: Int): MutableRasterData = t match {
    case TypeBit    => BitArrayRasterData.ofDim(cols, rows)
    case TypeByte   => ByteArrayRasterData.ofDim(cols, rows)
    case TypeShort  => ShortArrayRasterData.ofDim(cols, rows)
    case TypeInt    => IntArrayRasterData.ofDim(cols, rows)
    case TypeFloat  => FloatArrayRasterData.ofDim(cols, rows)
    case TypeDouble => DoubleArrayRasterData.ofDim(cols, rows)
  }

  def emptyByType(t: RasterType, cols: Int, rows: Int): MutableRasterData = t match {
    case TypeBit    => BitArrayRasterData.empty(cols, rows)
    case TypeByte   => ByteArrayRasterData.empty(cols, rows)
    case TypeShort  => ShortArrayRasterData.empty(cols, rows)
    case TypeInt    => IntArrayRasterData.empty(cols, rows)
    case TypeFloat  => FloatArrayRasterData.empty(cols, rows)
    case TypeDouble => DoubleArrayRasterData.empty(cols, rows)
  }
}



