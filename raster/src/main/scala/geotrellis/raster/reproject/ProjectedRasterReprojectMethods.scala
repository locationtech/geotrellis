package geotrellis.raster.reproject

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.proj4._

import spire.syntax.cfor._

class ProjectedRasterReprojectMethods[T <: CellGrid](val self: ProjectedRaster[T])(implicit ev: Raster[T] => RasterReprojectMethods[Raster[T]]) 
    extends MethodExtensions[ProjectedRaster[T]] {
  import Reproject.Options

  def reproject(dest: CRS, options: Options): ProjectedRaster[T] =
    ProjectedRaster(self.raster.reproject(self.crs, dest, options), dest)

  def reproject(dest: CRS): ProjectedRaster[T] =
    reproject(dest, Options.DEFAULT)

  /** Windowed */
  def reproject(gridBounds: GridBounds, dest: CRS, options: Options): ProjectedRaster[T] =
    ProjectedRaster(self.raster.reproject(gridBounds, self.crs, dest, options), dest)

  def reproject(gridBounds: GridBounds, dest: CRS): ProjectedRaster[T] =
    reproject(gridBounds, dest, Options.DEFAULT)
}
