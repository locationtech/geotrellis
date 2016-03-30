package geotrellis.raster.split

import geotrellis.raster.{RasterExtent, CellGrid, Raster}

object Implicits extends Implicits

trait Implicits {
  implicit class withRasterExtentSplitMethods(val self: RasterExtent) extends RasterExtentSplitMethods
  implicit class withRasterSplitMethods[T <: CellGrid: (? => SplitMethods[T])](val self: Raster[T]) extends RasterSplitMethods[T]
}
