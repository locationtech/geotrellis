/*
 * Copyright (c) 2016 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.vector.{Geometry,Extent}


trait TileRasterizeMethods[T <: Tile] extends MethodExtensions[T] {
  def foreachCell(geom : Geometry, extent : Extent, ie : Boolean = false)(fn : Int => Int) : Tile =
    Raster(self, extent).foreachCell(geom, ie)(fn)

  def foreachCellDouble(geom : Geometry, extent : Extent, ie : Boolean = false)(fn : Double => Double) : Tile =
    Raster(self, extent).foreachCellDouble(geom, ie)(fn)

  def foreachCellCovering(geom : Geometry, extent : Extent)(fn : Int => Int) : Tile =
    foreachCell(geom, extent, true)(fn)

  def foreachCellContainedBy(geom : Geometry, extent : Extent)(fn : Int => Int) : Tile =
    foreachCell(geom, extent, false)(fn)

  def foreachCellCoveringDouble(geom : Geometry, extent : Extent)(fn : Double => Double) : Tile =
    foreachCellDouble(geom, extent, true)(fn)

  def foreachCellContainedByDouble(geom : Geometry, extent : Extent)(fn : Double => Double) : Tile =
    foreachCellDouble(geom, extent, false)(fn)
}
