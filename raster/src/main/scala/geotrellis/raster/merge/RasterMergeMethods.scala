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
import geotrellis.vector.Extent
import geotrellis.util.MethodExtensions


/**
  * A class providing extension methods for merging rasters.
  */
class RasterMergeMethods[T <: CellGrid: ? => TileMergeMethods[T]](val self: Raster[T]) extends MethodExtensions[Raster[T]] {

  /**
    * Merge this [[Raster]] with the other one.  All places in the
    * present raster that contain NODATA are filled-in with data from
    * the other raster.  A new Raster is returned.
    *
    * @param   other         The other Raster
    * @param   method        The resampling method
    * @return                A new Raster, the result of the merge
    */
  def merge(other: Raster[T], method: ResampleMethod): Raster[T] =
    Raster(self.tile.merge(self.extent, other.extent, other.tile, method), self.extent)

  /**
    * Merge this [[Raster]] with the other one.  All places in the
    * present raster that contain NODATA are filled-in with data from
    * the other raster.  A new Raster is returned.
    *
    * @param   other        The other Raster
    * @return               A new Raster, the result of the merge
    */
  def merge(other: Raster[T]): Raster[T] =
    merge(other, NearestNeighbor)
}
