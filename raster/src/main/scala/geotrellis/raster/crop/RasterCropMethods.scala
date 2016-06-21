package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._


/**
  * A class containing extension methods for cropping [[Raster]]s.
  */
class RasterCropMethods[T <: CellGrid: (? => CropMethods[T])](val self: Raster[T]) extends CropMethods[Raster[T]] {
  import Crop.Options

  /**
    * Given an Extent and some cropping options, produce a cropped
    * [[Raster]].
    */
  def crop(extent: Extent, options: Options): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val gridBounds = re.gridBoundsFor(extent, clamp = options.clamp)
    val croppedExtent = re.extentFor(gridBounds, clamp = options.clamp)
    val croppedTile = self._1.crop(gridBounds, options)
    Raster(croppedTile, croppedExtent)
  }

  /**
    * Given an Extent, produce a cropped [[Raster]].
    */
  def crop(extent: Extent): Raster[T] =
    crop(extent, Options.DEFAULT)

  /**
    * Given a [[GridBounds]] and some cropping options, produce a new
    * [[Raster]].
    */
  def crop(gb: GridBounds, options: Options): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val croppedExtent = re.extentFor(gb)
    Raster(self._1.crop(gb, options), croppedExtent)
  }
}
