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
import geotrellis.vector.Geometry


trait RasterExtentRasterizeMethods[T <: RasterExtent] extends MethodExtensions[T] {
  def foreachCell(
    geom : Geometry,
    options: Options = Options.DEFAULT,
    ct : CellType = TypeInt
  )(fn : (Int, Int) => Int) : Tile =
    geom.foreachCell(self, options, ct)(fn)

  def foreachCellDouble(
    geom : Geometry,
    options: Options = Options.DEFAULT,
    ct : CellType = TypeDouble
  )(fn : (Int, Int) => Double) : Tile =
    geom.foreachCellDouble(self, options, ct)(fn)
}
