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

  def mutable():MutableRasterData = {
    val forcedData = RasterData.allocByType(getType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        forcedData.set(col,row,g(data.get(col,row)))
      }
    }
    forcedData
  }
  def force():RasterData = mutable

  final def apply(i: Int) = g(data(i))
  final def applyDouble(i: Int) = i2d(g(data(i)))
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

  def mutable():MutableRasterData = {
    val forcedData = RasterData.allocByType(getType,cols,rows)
    for(col <- 0 until cols optimized) {
      for(row <- 0 until rows optimized) {
        forcedData.setDouble(col,row,g(data.getDouble(col,row)))
      }
    }
    forcedData
  }
  def force():RasterData = mutable

  final def apply(i: Int) = d2i(g(data.applyDouble(i)))
  final def applyDouble(i: Int) = g(data.applyDouble(i))
}
