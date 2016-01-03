package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector._

trait TileResampleMethods[T <: CellGrid] extends MethodExtensions[T] {
  def resample(extent: Extent, target: RasterExtent, method: ResampleMethod): T

  def resample(extent: Extent, target: RasterExtent): T =
    resample(extent, target, ResampleMethod.DEFAULT)


  def resample(extent: Extent, target: Extent, method: ResampleMethod): T

  def resample(extent: Extent, target: Extent): T =
    resample(extent, target, ResampleMethod.DEFAULT)


  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): T

  def resample(extent: Extent, targetCols: Int, targetRows: Int): T =
    resample(extent, targetCols, targetRows, ResampleMethod.DEFAULT)

  def resample(targetCols: Int, targetRows: Int, method: ResampleMethod): T =
    resample(Extent(0.0, 0.0, 1000.0, 1000.0), targetCols, targetRows, method)

  def resample(targetCols: Int, targetRows: Int): T =
    resample(Extent(0.0, 0.0, 1000.0, 1000.0), targetCols, targetRows, ResampleMethod.DEFAULT)
}
