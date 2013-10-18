package geotrellis.raster

import geotrellis._

/**
 * LazyCombineDouble represents a lazily-applied combineDouble method.
 */
final case class LazyCombineDouble(data1: RasterData,
                                   data2: RasterData,
                                   g: (Double, Double) => Double) extends LazyRasterData {

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

  def apply(i: Int) = d2i(g(data1.applyDouble(i), data2.applyDouble(i)))
  def applyDouble(i: Int) = g(data1.applyDouble(i), data2.applyDouble(i))
  def copy = this

  def foreach(f: Int => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(d2i(g(data1.applyDouble(i), data2.applyDouble(i))))
      i += 1
    }
  }

  def map(f: Int => Int) = LazyCombine(data1, data2, (a, b) => f(d2i(g(i2d(a), i2d(b)))))

  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: RasterData => LazyCombine(this, a, f)
    case o                  => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f: Double => Unit) = {
    var i = 0
    val len = length
    while (i < len) {
      f(g(data1.applyDouble(i), data2.applyDouble(i)))
      i += 1
    }
  }

  def mapDouble(f: Double => Double) = {
    LazyCombineDouble(data1, data2, (a, b) => f(g(a, b)))
  }

  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: RasterData => LazyCombineDouble(this, a, f)
    case o                  => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
