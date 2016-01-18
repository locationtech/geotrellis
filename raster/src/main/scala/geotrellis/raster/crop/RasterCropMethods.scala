package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

class RasterCropMethods[T <: CellGrid: (? => CropMethods[T])](val self: Raster[T]) extends CropMethods[Raster[T]] {
  import Crop.Options

  def crop(extent: Extent, options: Options): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val gridBounds = re.gridBoundsFor(extent, clamp = options.clamp)
    val croppedExtent = re.extentFor(gridBounds, clamp = options.clamp)
    val croppedTile = self._1.crop(gridBounds, options)
    Raster(croppedTile, croppedExtent)
  }

  def crop(extent: Extent): Raster[T] =
    crop(extent, Options.DEFAULT)

  def crop(gb: GridBounds, options: Options): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val croppedExtent = re.extentFor(gb)
    Raster(self._1.crop(gb, options), croppedExtent)
  }
}
