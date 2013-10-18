package geotrellis.raster

import geotrellis._

/**
 *
 * NOTE: col1 and row1 (upper left) is inclusive, while col2 and row2 (lower
 * right) is not. This means that the actual size of the "real data" here is
 * just col2 - col1, and row2 - row1.
 */
final case class MaskedRasterData(data:RasterData, col1:Int, row1:Int, col2:Int, row2:Int)
extends LazyRasterData with Wrapper {
  def copy = this
  def underlying = data
  override def toArray = data.toArray
  override def toArrayDouble = data.toArrayDouble

  final override def get(col:Int, row:Int) = {
    if (col < col1 || col2 <= col || row < row1 || row <= row2) NODATA
    else underlying.get(col, row)
  }

  final override def getDouble(col:Int, row:Int) = {
    if (col < col1 || col2 <= col || row < row1 || row <= row2) NODATA
    else underlying.getDouble(col, row)
  }

  final def apply(i:Int) = {
    val row = i / data.cols
    val col = i % data.cols
    if (col < col1 || col2 <= col || row < row1 || row2 <= row) NODATA
    else underlying.apply(i)
  }

  final def applyDouble(i:Int) = {
    val row = i / data.cols
    val col = i % data.cols
    if (col < col1 || col2 <= col || row < row1 || row <= row2) NODATA
    else underlying.applyDouble(i)
  }

  def foreach(f:Int => Unit) = {
    var row = row1
    while (row < row2) {
      val span = row * cols
      var col = col1
      while (col < col2) {
        f(underlying.apply(span + col))
        col += 1
      }
      row += 1
    }
  }

  def map(f:Int => Int) = LazyMap(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:RasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }


  def foreachDouble(f:Double => Unit) = {
    var row = row1
    while (row < row2) {
      val span = row * cols
      var col = col1
      while (col < col2) {
        f(underlying.applyDouble(span + col))
        col += 1
      }
      row += 1
    }
  }

  def mapDouble(f:Double => Double) = LazyMapDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:RasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
