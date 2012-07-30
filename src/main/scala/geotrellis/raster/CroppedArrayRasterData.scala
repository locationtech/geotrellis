package geotrellis.raster

import geotrellis._

object CroppedArrayRasterData {
  
}

/**
 * This trait represents a raster data which represents a lazily-applied
 * cropping of an underlying raster data object.
 */
case class CroppedArrayRasterData(underlying:ArrayRasterData,
                                  rasterExtent:RasterExtent,
                                  colOffset:Int, rowOffset:Int,
                                  cols:Int, rows:Int) extends ArrayRasterData {
  def length = cols * rows

  // TODO: this will be slow
  def mutable:Option[MutableRasterData] = {
    val arr = alloc(cols, rows)
    if (isFloat) {
      for (c <- 0 until cols; r <- 0 until rows) {
        arr.setDouble(c, r, getDouble(c, r))
      }
    } else {
      for (c <- 0 until cols; r <- 0 until rows) {
        arr.set(c, r, get(c, r))
      }
    }
    Option(arr)
  }
  def force = mutable

  def getType = underlying.getType
  def copy = this
  def alloc(cols:Int, rows:Int) = underlying.alloc(cols, rows)

  def apply(i:Int) = get(i % cols, i / cols)
  def applyDouble(i:Int) = getDouble(i % cols, i / cols)

  override def get(col:Int, row:Int):Int = {
    val c = col + colOffset
    if (c < 0 || c >= underlying.cols) return NODATA
    val r = row + rowOffset
    if (r < 0 || r >= underlying.rows) return NODATA
    underlying.get(c, r)
  }

  override def getDouble(col:Int, row:Int):Double = {
    val c = col + colOffset
    if (c < 0 || c >= underlying.cols) return Double.NaN
    val r = row + rowOffset
    if (r < 0 || r >= underlying.rows) return Double.NaN
    underlying.getDouble(c, r)
  }

  def foreach(f:Int => Unit) =
    for (c <- 0 until cols; r <- 0 until rows) f(get(c, r))

  def map(f:Int => Int) = LazyMap(this, f)
  def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) =
    for (c <- 0 until cols; r <- 0 until rows) f(getDouble(c, r))

  def mapDouble(f:Double => Double) = LazyMapDouble(this, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
