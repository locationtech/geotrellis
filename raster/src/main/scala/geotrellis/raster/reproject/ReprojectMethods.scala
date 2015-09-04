package geotrellis.raster.reproject

import geotrellis.proj4.CRS
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent

trait ReprojectMethods[T] {
  type ReturnType

  def reproject(extent: Extent, src: CRS, dest: CRS, options: ReprojectOptions): ReturnType

  def reproject(extent: Extent, src: CRS, dest: CRS): ReturnType =
    reproject(extent, src, dest, ReprojectOptions.DEFAULT)

  def reproject(method: ResampleMethod, extent: Extent, src: CRS, dest: CRS): ReturnType =
    reproject(extent, src, dest, ReprojectOptions(method = method))
}
