package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.vector.{Extent, ProjectedExtent}
import geotrellis.proj4.CRS

object ProjectedRaster {
  implicit def tupToRaster[T <: CellGrid](tup: (Raster[T], CRS)): ProjectedRaster[T] =
    ProjectedRaster(tup._1, tup._2)

  implicit def projectedToRaster[T <: CellGrid](p: ProjectedRaster[T]): Raster[T] =
    p.raster

  implicit def projectedToTile[T <: CellGrid](p: ProjectedRaster[T]): T =
    p.raster.tile

  def apply[T <: CellGrid](tile: T, extent: Extent, crs: CRS): ProjectedRaster[T] =
    ProjectedRaster(Raster(tile, extent), crs)

}

case class ProjectedRaster[T <: CellGrid](raster: Raster[T], crs: CRS) {
  def tile = raster.tile
  def extent = raster.extent
  def projectedExtent = ProjectedExtent(extent, crs)
}
