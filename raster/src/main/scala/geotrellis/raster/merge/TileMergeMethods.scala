package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions


trait TileMergeMethods[T] extends MethodExtensions[T] {
  def merge(other: T): T
  def merge(extent: Extent, otherExtent: Extent, other: T, method: ResampleMethod): T
  def merge(extent: Extent, otherExtent: Extent, other: T): T =
    merge(extent, otherExtent, other, NearestNeighbor)
}
