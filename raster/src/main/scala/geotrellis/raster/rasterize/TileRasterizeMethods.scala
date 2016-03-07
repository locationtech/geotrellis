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
import geotrellis.raster.rasterize.Rasterize.Options
import geotrellis.vector.{Geometry,Extent}
import geotrellis.util.MethodExtensions


trait TileRasterizeMethods[T <: Tile] extends MethodExtensions[T] {
  def rasterize(
    geom: Geometry,
    extent: Extent,
    options: Options = Options.DEFAULT
  )(fn : Int => Int): Raster[MutableArrayTile] =
    Raster(self, extent).rasterize(geom, options)(fn)

  def rasterizeDouble(
    geom: Geometry,
    extent: Extent,
    options: Options = Options.DEFAULT
  )(fn : Double => Double): Raster[MutableArrayTile] =
    Raster(self, extent).rasterizeDouble(geom, options)(fn)
}
