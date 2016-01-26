package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector.Extent

class RasterMergeMethods[T <: CellGrid: ? => TileMergeMethods[T]](val self: Raster[T]) extends MethodExtensions[Raster[T]] {
  def merge(other: Raster[T], method: ResampleMethod): Raster[T] =
    Raster(self.tile.merge(self.extent, other.extent, other.tile, method), self.extent)

  def merge(other: Raster[T]): Raster[T] =
    merge(other, NearestNeighbor)
}
