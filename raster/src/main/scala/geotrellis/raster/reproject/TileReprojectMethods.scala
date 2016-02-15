package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.proj4._
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions


trait TileReprojectMethods[T <: CellGrid] extends MethodExtensions[T] {
  import Reproject.Options

  def reproject(srcExtent: Extent, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform, options: Options): Raster[T]

  def reproject(srcExtent: Extent, targetRasterExtent: RasterExtent, transform: Transform, inverseTransform: Transform): Raster[T] =
    reproject(srcExtent, targetRasterExtent, transform, inverseTransform, Options.DEFAULT)

  def reproject(srcExtent: Extent, src: CRS, dest: CRS, options: Options): Raster[T]

  def reproject(srcExtent: Extent, src: CRS, dest: CRS): Raster[T] =
    reproject(srcExtent, src, dest, Options.DEFAULT)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS, options: Options): Raster[T]

  def reproject(srcExtent: Extent, gridBounds: GridBounds, src: CRS, dest: CRS): Raster[T] =
    reproject(srcExtent, gridBounds, src, dest, Options.DEFAULT)

  def reproject(srcExtent: Extent, gridBounds: GridBounds, transform: Transform, inverseTransform: Transform, options: Options): Raster[T]

  def reproject(srcExtent: Extent, gridBounds: GridBounds, transform: Transform, inverseTransform: Transform): Raster[T] =
    reproject(srcExtent, gridBounds, transform, inverseTransform, Options.DEFAULT)

}
