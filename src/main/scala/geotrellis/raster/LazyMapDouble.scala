package geotrellis.raster

import geotrellis._

/**
 * LazyMapDouble represents a lazily-applied mapDouble method.
 */
final case class LazyMapDouble(data: ArrayRasterData, g: Double => Double)
  extends LazyRasterData with Wrapper {

  def copy = this
  def underlying = data

  final def apply(i: Int) = d2i(g(data.applyDouble(i)))
  final def applyDouble(i: Int) = g(data.applyDouble(i))

  def foreach(f: Int => Unit) = data.foreachDouble(z => f(d2i(g(z))))
  def map(f: Int => Int) = LazyMap(data, z => f(d2i(g(i2d(z)))))
  def mapIfSet(f: Int => Int) = LazyMapIfSet(this, f)
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(d2i(g(i2d(z1))), z2))
    case o                  => o.combine(data)((z2, z1) => f(d2i(g(i2d(z1))), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreachDouble(z => f(g(z)))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(g(z)))
  def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(g(z1), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(g(z1), z2))
  }
}