package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait ExtentCropMethods[T <: CellGrid, R <: Product2[T, Extent]] extends MethodExtensions[R] {
  def crop(extent: Extent, force: Boolean)(implicit ev: T => CropMethods[T], ev2: ((T, Extent)) => R): R =
    (self._1.crop(RasterExtent(self._2, self._1).gridBoundsFor(extent), force), extent)

  def crop(extent: Extent)(implicit ev: T => CropMethods[T], ev2: ((T, Extent)) => R): R =
    crop(extent, false)
}
