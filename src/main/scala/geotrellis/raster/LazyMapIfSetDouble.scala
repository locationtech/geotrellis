package geotrellis.raster

import geotrellis._

/**
 * LazyMapIfSetDouble represents a lazily-applied mapIfSet method.
 */
final case class LazyMapIfSetDouble(data: ArrayRasterData, g: Double => Double)
  extends LazyRasterData with Wrapper {
  def underlying = data
  def copy = this

  def gIfSet(z: Double) = if (isNodata(z)) Double.NaN else g(z)

  final def apply(i: Int) = d2i(gIfSet(data(i)))
  final def applyDouble(i: Int) = gIfSet(data(i))

  def foreach(f: Int => Unit) = data.foreachDouble(z => f(d2i(gIfSet(z))))
  def map(f: Int => Int) = LazyMap(data, z => f(d2i(gIfSet(i2d(z)))))
  def mapIfSet(f: Int => Int) = LazyMapIfSet(data, z => f(d2i(g(i2d(z)))))
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(d2i(gIfSet(i2d(z1))), z2))
    case o                  => o.combine(data)((z2, z1) => f(d2i(gIfSet(i2d(z1))), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreachDouble(z => f(gIfSet(z)))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(gIfSet(z)))
  def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(data, z => f(g(z)))
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(gIfSet(z1), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(gIfSet(z1), z2))
  }
}
