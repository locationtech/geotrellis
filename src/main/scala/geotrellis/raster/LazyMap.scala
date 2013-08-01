package geotrellis.raster

import geotrellis._

/**
 * LazyMap represents a lazily-applied map method.
 */
final case class LazyMap(data: ArrayRasterData, g: Int => Int)
  extends LazyRasterData with Wrapper {

  def copy = this
  def underlying = data

  final def apply(i: Int) = g(data(i))
  final def applyDouble(i: Int) = i2d(g(data(i)))

  def foreach(f: Int => Unit) = data.foreach(z => f(g(z)))
  def map(f: Int => Int) = LazyMap(data, z => f(g(z)))
  def mapIfSet(f: Int => Int) = LazyMapIfSet(this, f)
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: ArrayRasterData => LazyCombine(data, a, (z1, z2) => f(g(z1), z2))
    case o                  => o.combine(data)((z2, z1) => f(g(z1), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreach(z => f(i2d(g(z))))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(i2d(g(d2i(z)))))
  def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: ArrayRasterData => LazyCombineDouble(data, a, (z1, z2) => f(i2d(g(d2i(z1))), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(i2d(g(d2i(z1))), z2))
  }
}