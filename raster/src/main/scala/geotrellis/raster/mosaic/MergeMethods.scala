package geotrellis.raster.mosaic

import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector.Extent

trait MergeMethods[T] {
  val tile: T
  def merge(other: T): T
  def merge(extent: Extent, otherExtent: Extent, other: T, method: ResampleMethod): T
  def merge(extent: Extent, otherExtent: Extent, other: T): T =
    merge(extent, otherExtent, other, NearestNeighbor)
}