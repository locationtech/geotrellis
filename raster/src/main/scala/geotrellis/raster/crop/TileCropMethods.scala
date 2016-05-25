package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._


/**
  * A trait guaranteeing extension methods for cropping [[Tile]]s.
  */
trait TileCropMethods[T <: CellGrid] extends CropMethods[T] {
  import Crop.Options

  /**
    * Given a source Extent, a destination extent, and some cropping
    * options, produce a cropped [[Tile]].
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): T

  /**
    * Given a source Extent and a destination extent produce a cropped
    * [[Tile]].
    */
  def crop(srcExtent: Extent, extent: Extent): T =
    crop(srcExtent, extent, Options.DEFAULT)

}
