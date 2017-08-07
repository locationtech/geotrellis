/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.merge

import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.util.MethodExtensions
import geotrellis.vector.Extent


/**
  * Trait guaranteeing extension methods for doing merge operations on [[Tile]]s.
  */
trait TileMergeMethods[T] extends MethodExtensions[T] {

  /** Merges this tile with another tile.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note                         This method requires that the dimensions be the same between the tiles, and assumes
    *                               equal extents.
    */
  def merge(other: T): T

  /** Merge this tile with another for given bounds.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note It is assumed that other tile cell (0,0) lines up with (0,0) of this tile.
    *
    * @param other     Source of cells to be merged
    * @param bounds    Cell grid bounds in target tile for merge operation
    */
  def merge(other: T, bounds: GridBounds): T

  /** Merge this tile with another for given bounds.
    *
    * This method will replace the values of these cells with the
    * values of the other tile's corresponding cells, if the source
    * cell is of the transparent value.  The transparent value is
    * determined by the tile's cell type; if the cell type has a
    * NoData value, then that is considered the transparent value.  If
    * there is no NoData value associated with the cell type, then a 0
    * value is considered the transparent value. If this is not the
    * desired effect, the caller is required to change the cell type
    * before using this method to an appropriate cell type that has
    * the desired NoData value.
    *
    * @note
    * Offset of (0,0) indicates that upper left corner of other tile lines up with
    * upper left corner of the merge bounds. Incrementing the offset will have the
    * effect of moving other tile up and to the left relative to this tile.
    *
    * @param other     Source of cells to be merged
    * @param bounds    Cell grid bounds in target tile for merge operation
    * @param colOffset Offset of row 0 in other tile relative to merge bounds
    * @param rowOffset Offset or col 0 in other tile relative to merge bounds
    */
  def merge(other: T, bounds: GridBounds, colOffset: Int, rowOffset: Int): T

  /**
    * Merge this [[Tile]] with the other one.  All places in the
    * present tile that contain NODATA and are in the intersection of
    * the two given extents are filled-in with data from the other
    * tile.  A new Tile is returned.
    *
    * @param   extent        The extent of this tile
    * @param   otherExtent   The extent of the other tile
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
