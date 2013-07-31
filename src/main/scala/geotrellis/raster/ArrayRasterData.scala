package geotrellis.raster

import geotrellis._
import geotrellis.raster.RasterUtil._

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
