package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent


/**
  * Trait guaranteeing extension methods for doing merge operations on [[Tile]]s.
  */
trait TileMergeMethods[T] extends MethodExtensions[T] {

  /**
    * Merge this [[Tile]] with the other one.  All places in the
    * present tile that contain NODATA are filled-in with data from
    * the other tile.  A new Tile is returned.
    *
    * @param   other        The other Tile
    * @return               A new Tile, the result of the merge
    */
  def merge(other: T): T

  /**
    * Merge this [[Tile]] with the other one.  All places in the
    * present tile that contain NODATA and are in the intersection of
    * the two given extents are filled-in with data from the other
    * tile.  A new Tile is returned.
    *
    * @param   extent        The extent of this Tile
    * @param   otherExtent   The extent of the other Tile
    * @param   other         The other Tile
    * @param   method        The resampling method
    * @return                A new Tile, the result of the merge
    */
  def merge(extent: Extent, otherExtent: Extent, other: T, method: ResampleMethod): T

  /**
    * Merge this [[Tile]] with the other one.  All places in the
    * present tile that contain NODATA and are in the intersection of
    * the two given extents are filled-in with data from the other
    * tile.  A new Tile is returned.
    *
    * @param   extent        The extent of this Tile
    * @param   otherExtent   The extent of the other Tile
    * @param   other         The other Tile
    * @return                A new Tile, the result of the merge
    */
  def merge(extent: Extent, otherExtent: Extent, other: T): T =
    merge(extent, otherExtent, other, NearestNeighbor)
}
