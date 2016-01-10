package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait TileCropMethods[T <: CellGrid] extends CropMethods[T] {
  import Crop.Options

  def crop(srcExtent: Extent, extent: Extent, options: Options): T

  def crop(srcExtent: Extent, extent: Extent): T =
    crop(srcExtent, extent, Options.DEFAULT)

}
