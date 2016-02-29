package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions


trait RasterResampleMethods[+T <: Raster[_]] extends MethodExtensions[T] {
  def resample(target: RasterExtent, method: ResampleMethod): T

  def resample(target: RasterExtent): T =
    resample(target, ResampleMethod.DEFAULT)

  def resample(targetCols: Int, targetRows: Int, method: ResampleMethod): T =
    resample(RasterExtent(self.extent, targetCols, targetRows), method)

  def resample(targetCols: Int, targetRows: Int): T =
    resample(targetCols, targetRows, ResampleMethod.DEFAULT)

}
