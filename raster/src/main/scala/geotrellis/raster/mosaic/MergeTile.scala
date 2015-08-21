package geotrellis.raster.mosaic


import geotrellis.raster.CellType
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.vector.Extent

trait MergeTile[T] {
  val tile: T
  def merge(other: T): T
  def merge(extent: Extent, otherExtent: Extent, other: T, method: ResampleMethod): T
  def merge(extent: Extent, otherExtent: Extent, other: T): T =
    merge(extent, otherExtent, other, NearestNeighbor)
}

trait BlankTile[T]{
  def makeFrom(prototype: T, cellType: CellType, cols: Int, rows: Int): T
}

