package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent

trait RasterReprojectMethods[+T <: Raster[_]] extends MethodExtensions[T] {
  import Reproject.Options

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): T

  def reproject(targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): T =
    reproject(targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(src: CRS, dest: CRS, options: Options): T = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    val targetRasterExtent = ReprojectRasterExtent(self.rasterExtent, transform)

    reproject(targetRasterExtent, transform, inverseTransform, options)
  }

  def reproject(src: CRS, dest: CRS): T =
    reproject(src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): T = {
    val transform = Transform(src, dest)
    val inverseTransform = Transform(dest, src)

    reproject(gridBounds, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, src: CRS, dest: CRS): T =
    reproject(gridBounds, src, dest, Options.DEFAULT)

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): T = {
    val rasterExtent = self.rasterExtent
    val windowExtent = rasterExtent.extentFor(gridBounds)
    val windowRasterExtent = RasterExtent(windowExtent, gridBounds.width, gridBounds.height)
    val targetRasterExtent = ReprojectRasterExtent(windowRasterExtent, transform)

    reproject(targetRasterExtent, transform, inverseTransform, options)
  }

  def reproject(gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): T =
    reproject(gridBounds, transform, inverseTransform, Options.DEFAULT)

}
