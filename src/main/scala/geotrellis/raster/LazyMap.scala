package geotrellis.raster

import geotrellis._

import scalaxy.loops._

/**
 * LazyMap represents a lazily-applied map method.
 */
final case class LazyMap(data: RasterData, g: Int => Int)
  extends RasterData {

  final def getType = data.getType
  final def alloc(cols: Int, rows: Int) = data.alloc(cols, rows)
  final def length = data.length

  def cols = data.cols
  def rows = data.rows

  def copy = force

  def force():RasterData = {
    val data = RasterData.allocByType(getType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.set(col,row,g(data.get(col,row)))
      }
    }
    data
  }

  final def apply(i: Int) = g(data(i))
  final def applyDouble(i: Int) = i2d(g(data(i)))

  def foreach(f: Int => Unit) = data.foreach(z => f(g(z)))
  def map(f: Int => Int) = LazyMap(data, z => f(g(z)))
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: RasterData => LazyCombine(data, a, (z1, z2) => f(g(z1), z2))
    case o                  => o.combine(data)((z2, z1) => f(g(z1), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreach(z => f(i2d(g(z))))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(i2d(g(d2i(z)))))
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: RasterData => LazyCombineDouble(data, a, (z1, z2) => f(i2d(g(d2i(z1))), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(i2d(g(d2i(z1))), z2))
  }
}

/**
 * LazyMapDouble represents a lazily-applied mapDouble method.
 */
final case class LazyMapDouble(data: RasterData, g: Double => Double)
  extends RasterData {

  final def getType = data.getType
  final def alloc(cols: Int, rows: Int) = data.alloc(cols, rows)
  final def length = data.length

  def cols = data.cols
  def rows = data.rows

  def copy = force

  def force():RasterData = {
    val data = RasterData.allocByType(getType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        data.setDouble(col,row,g(data.getDouble(col,row)))
      }
    }
    data
  }

  final def apply(i: Int) = d2i(g(data.applyDouble(i)))
  final def applyDouble(i: Int) = g(data.applyDouble(i))

  def foreach(f: Int => Unit) = data.foreachDouble(z => f(d2i(g(z))))
  def map(f: Int => Int) = LazyMap(data, z => f(d2i(g(i2d(z)))))
  def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
    case a: RasterData => LazyCombine(data, a, (z1, z2) => f(d2i(g(i2d(z1))), z2))
    case o                  => o.combine(data)((z2, z1) => f(d2i(g(i2d(z1))), z2))
  }

  def foreachDouble(f: Double => Unit) = data.foreachDouble(z => f(g(z)))
  def mapDouble(f: Double => Double) = LazyMapDouble(data, z => f(g(z)))
  def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
    case a: RasterData => LazyCombineDouble(data, a, (z1, z2) => f(g(z1), z2))
    case o                  => o.combineDouble(data)((z2, z1) => f(g(z1), z2))
  }
}
