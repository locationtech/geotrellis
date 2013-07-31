package geotrellis.raster

import geotrellis._

/**
 * LazyCombine represents a lazily-applied combine method.
 */
final case class LazyCombine(data1: ArrayRasterData,
                             data2: ArrayRasterData,
                             g: (Int, Int) => Int) extends LazyRasterData {
  if (data1.lengthLong != data2.lengthLong) {
    val size1 = s"${data1.cols} x ${data1.rows}"
    val size2 = s"${data2.cols} x ${data2.rows}"
    sys.error(s"Cannot combine rasters of different sizes: $size1 vs $size2")
  }

  def cols = data1.cols
  def rows = data1.rows

  def getType = RasterData.largestType(data1, data2)
  def alloc(cols: Int, rows: Int) = RasterData.largestAlloc(data1, data2, cols, rows)
  def length = data1.length

  def apply(i: Int) = g(data1.apply(i), data2.apply(i))
  def applyDouble(i: Int) = i2d(g(data1.apply(i), data2.apply(i)))
  def copy = this

  def foreach(f: Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(g(data1(i), data2(i)))
      i += 1
    }
  }

  def map(f: Int => Int) = LazyCombine(data1, data2, (a, b) => f(g(a, b)))

  def mapIfSet(f: Int => Int) = {
    def h(a: Int, b: Int) = {
      val z = g(a, b)
      if (isNodata(z)) NODATA else f(z)
    }
    LazyCombine(data1, data2, h)
  }

  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(this, a, f)
    case o                  => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(i2d(g(data1(i), data2(i))))
      i += 1
    }
  }

  def mapDouble(f: Double => Double) = {
    LazyCombineDouble(data1, data2, (a, b) => f(i2d(g(d2i(a), d2i(b)))))
  }

  def mapIfSetDouble(f: Double => Double) = {
    def h(a: Double, b: Double) = {
      val z = g(d2i(a), d2i(b))
      if (isNodata(z)) Double.NaN else f(i2d(z))
    }
    LazyCombineDouble(data1, data2, h)
  }

  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(this, a, f)
    case o                  => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
