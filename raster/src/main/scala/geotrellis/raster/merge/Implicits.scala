package geotrellis.raster.merge

import geotrellis.raster._

object Implicits extends Implicits

trait Implicits {
  implicit class withRasterMergeMethods[T <: CellGrid: ? => TileMergeMethods[T]](self: Raster[T]) extends RasterMergeMethods[T](self)
}
