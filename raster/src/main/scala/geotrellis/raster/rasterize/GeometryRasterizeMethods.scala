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
import geotrellis.util.MethodExtensions


trait GeometryRasterizeMethods[T <: Geometry] extends MethodExtensions[T] {

  def foreachCell(
    re : RasterExtent,
    options: Options = Options.DEFAULT,
    ct : CellType = IntConstantNoDataCellType
  )(fn : (Int, Int) => Int) : Tile = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    Rasterizer.foreachCellByGeometry(self, re, options)({ (x,y) => tile.set(x,y,fn(x,y)) })
    tile
  }

  def foreachCellDouble(
    re : RasterExtent,
    options: Options = Options.DEFAULT,
    ct : CellType = DoubleConstantNoDataCellType
  )(fn : (Int, Int) => Double) : Tile = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    Rasterizer.foreachCellByGeometry(self, re, options)({ (x,y) => tile.setDouble(x,y,fn(x,y)) })
    tile
  }

  def rasterize(re : RasterExtent)(fn : (Int, Int) => Int) =
    Rasterizer.rasterize(self, re)(fn)
}
