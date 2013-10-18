package geotrellis.raster

import geotrellis._

/**
 * Wrapper is a mixin which implements some RasterData methods in terms of an
 * underlying raster data.
 */
trait Wrapper {
  protected[this] def underlying: RasterData
  final def getType = underlying.getType
  final def alloc(cols: Int, rows: Int) = underlying.alloc(cols, rows)
  final def length = underlying.length

  def cols = underlying.cols
  def rows = underlying.rows
}
