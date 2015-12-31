package geotrellis.raster.reproject

import geotrellis.proj4.CRS
import geotrellis.raster.resample.ResampleMethod
import geotrellis.vector.Extent

trait ReprojectMethods[T] {
  type ReturnType <: Product2[T, Extent]

  def reproject(extent: Extent, src: CRS, dest: CRS): Reproject.Apply[ReturnType]
}
