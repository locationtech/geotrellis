package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

trait ResampleMethods[T <: CellGrid] extends MethodExtensions[T] {
  def resample(extent: Extent, targetExtent: RasterExtent, method: ResampleMethod): T

  def resample(source: Extent, target: RasterExtent): T =
    resample(source, target, ResampleMethod.DEFAULT)

  def resample(source: Extent, target: Extent): T =
    resample(source, target, ResampleMethod.DEFAULT)

  def resample(extent: Extent, targetExtent: Extent, method: ResampleMethod): T =
    resample(extent, RasterExtent(extent, self.cols, self.rows).createAligned(targetExtent), method)

  def resample(extent: Extent, targetCols: Int, targetRows: Int): T =
    resample(extent, targetCols, targetRows, ResampleMethod.DEFAULT)

  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): T =
    resample(extent, RasterExtent(extent, targetCols, targetRows), method)

  /** Only changes the resolution */
  def resample(targetCols: Int, targetRows: Int): T =
    resample(Extent(0.0, 0.0, 1.0, 1.0), targetCols, targetRows)
}
