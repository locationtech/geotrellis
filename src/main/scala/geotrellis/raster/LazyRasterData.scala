package geotrellis.raster

import geotrellis._

/**
 * LazyRasterData is an ArrayRasterData which (may) be lazily evaluated, and
 * which will perform other operations lazily.
 */
trait LazyRasterData extends ArrayRasterData {
  def force = mutable

  def mutable = {
    val len = length
    val d = alloc(cols, rows)
    var i = 0
    while (i < len) {
      d(i) = apply(i)
      i += 1
    }
    Option(d)
  }
}