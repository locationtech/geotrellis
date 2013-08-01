package geotrellis.raster

import geotrellis._

/**
 * LazyMapIfSet represents a lazily-applied mapIfSet method.
 */
final case class LazyMapIfSet(data: ArrayRasterData, g: Int => Int)
  extends LazyRasterData with Wrapper {
  def underlying = data
  def copy = this

  def gIfSet(z: Int) = if (isNodata(z)) NODATA else g(z)

  final def apply(i: Int) = gIfSet(data(i))
  final def applyDouble(i: Int) = i2d(gIfSet(data(i)))

  def foreach(f: Int => Unit) = data.foreach(z => f(gIfSet(z)))
  def map(f: Int => Int) = LazyMap(data, z => f(gIfSet(z)))
  def mapIfSet(f: Int => Int) = LazyMapIfSet(data, z => f(g(z)))
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(gIfSet(z1), z2))
    case o                  => o.combine(data)((z2, z1) => f(gIfSet(z1), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreach(z => f(i2d(gIfSet(z))))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(i2d(gIfSet(d2i(z)))))
  def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(data, z => f(i2d(g(d2i(z)))))
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(i2d(gIfSet(d2i(z1))), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(i2d(gIfSet(d2i(z1))), z2))
  }
}
