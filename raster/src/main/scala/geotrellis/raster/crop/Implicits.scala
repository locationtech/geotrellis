package geotrellis.raster.crop

import geotrellis.raster._
import geotrellis.vector._

object Implicits extends Implicits

/**
  * Trait housing the implicit class which add extension methods for
  * cropping to [[CellGrid]].
  */
trait Implicits {
  implicit class withExtentCropMethods[T <: CellGrid: (? => CropMethods[T])](self: Raster[T])
      extends RasterCropMethods[T](self)
}
