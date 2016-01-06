package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

class ExtentCropMethods[T <: CellGrid: (? => CropMethods[T])](val self: Raster[T]) extends CropMethods[Raster[T]] {
  def crop(extent: Extent, force: Boolean): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val gridBounds = re.gridBoundsFor(extent)
    val croppedExtent = re.extentFor(gridBounds)
    val croppedTile = self._1.crop(gridBounds, force)
    Raster(croppedTile, croppedExtent)
  }

  def crop(extent: Extent): Raster[T] =
    crop(extent, false)

  def crop(gb: GridBounds, force: Boolean): Raster[T] = {
    val re = RasterExtent(self._2, self._1)
    val croppedExtent = re.extentFor(gb)
    Raster(self._1.crop(gb, force), croppedExtent)
  }
}
