package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent

trait TileReprojectMethods[T] extends MethodExtensions[T] {
  import Reproject.Options

  def reproject(srcExtent: Extent, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): Product2[T, Extent]

  def reproject(srcExtent: Extent, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): Product2[T, Extent] =
    reproject(srcExtent, targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(srcExtent: Extent, src: CRS, dest: CRS, options: Options): Product2[T, Extent]

  def reproject(srcExtent: Extent, src: CRS, dest: CRS): Product2[T, Extent] =
    reproject(srcExtent, src, dest, Options.DEFAULT)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): Product2[T, Extent]

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS): Product2[T, Extent] =
    reproject(srcExtent, gridBounds, src, dest, Options.DEFAULT)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): Product2[T, Extent]

  def reproject(srcExtent: Extent, gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): Product2[T, Extent] =
    reproject(srcExtent, gridBounds, transform, inverseTransform, Options.DEFAULT)

}
