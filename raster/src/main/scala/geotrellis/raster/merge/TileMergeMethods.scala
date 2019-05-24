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

  /**
    * Merge this [[Tile]] with the other one.  All places in the
    * present tile that contain NODATA are filled-in with data from
    * the other tile.  A new Tile is returned.
    *
    * @param   other        The other Tile
    * @return               A new Tile, the result of the merge
    */
  def merge(other: T): T = merge(other, 0, 0)

  /**
    * Merge this [[Tile]] with the other.  Places the content of the (i, j)th pixel
    * of other into the (col + i, row + j)th pixel of the result.
    *
    * @param   other        The other Tile
    * @param   col          The column in the output tile corresponding to the left
    *                       edge of the merged data
    * @param   row          The row in the output tile corresponding to the top edge
    *                       of the merged data
    * @return               The result of the merge
    */
  def merge(other: T, col: Int, row: Int): T

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
