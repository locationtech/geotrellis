package geotrellis.raster.crop

import geotrellis.raster._
import geotrellis.vector._

object Implicits extends Implicits

trait Implicits {
  implicit class withExtentCropMethods[T <: CellGrid: (? => CropMethods[T])](self: Raster[T])
      extends ExtentCropMethods(self)
}
