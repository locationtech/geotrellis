package geotrellis.raster.reproject

import geotrellis.raster.MethodExtensions
import geotrellis.proj4.CRS
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent

trait ReprojectMethods[T] extends MethodExtensions[T] {
  def reproject(src: CRS, dest: CRS): T
}
