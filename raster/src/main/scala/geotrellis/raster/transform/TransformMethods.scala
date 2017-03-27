package geotrellis.raster.transform

import geotrellis.raster.CellGrid
import geotrellis.util.MethodExtensions

trait TransformMethods[T <: CellGrid] extends MethodExtensions[T] {
  def rotate90(n: Int): T
  def flipVertical: T
  def flipHorizontal: T
  def zscore: T
}
