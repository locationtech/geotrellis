package geotrellis.raster

import geotrellis.proj4.CRS
import geotrellis.raster.reproject._
import geotrellis.vector.{Extent, ProjectedExtent}


/**
  * The companion object for the [[ProjectedRaster]] type.
  */
object ProjectedRaster {
  /**
    * Implicit conversion from a [[Raster]], CRS pair to a
    * [[ProjectedRaster]].
    */
  implicit def tupToRaster[T <: CellGrid](tup: (Raster[T], CRS)): ProjectedRaster[T] =
    ProjectedRaster(tup._1, tup._2)

  /**
    * Implicit conversion from a [[ProjectedRaster]] to a [[Raster]].
    */
  implicit def projectedToRaster[T <: CellGrid](p: ProjectedRaster[T]): Raster[T] =
    p.raster

  /**
    * Implicit conversion from a [[ProjectedRaster]] to a tile.
    */
  implicit def projectedToTile[T <: CellGrid](p: ProjectedRaster[T]): T =
    p.raster.tile

  /**
    * Take a [[Tile]], and Extent, and a CRS and use them to produce a
    * [[ProjectedRaster]].
    */
  def apply[T <: CellGrid](tile: T, extent: Extent, crs: CRS): ProjectedRaster[T] =
    ProjectedRaster(Raster(tile, extent), crs)
}

/**
  * The [[ProjectedRaster]] type.
  */
case class ProjectedRaster[T <: CellGrid](raster: Raster[T], crs: CRS) {
  def tile = raster.tile
  def extent = raster.extent
  def projectedExtent = ProjectedExtent(extent, crs)
}
