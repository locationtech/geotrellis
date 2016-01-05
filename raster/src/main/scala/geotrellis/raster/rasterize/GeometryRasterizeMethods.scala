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
import geotrellis.vector.Geometry


trait GeometryRasterizeMethods[T <: Geometry] extends MethodExtensions[T] {

  def foreachCell(re : RasterExtent, ie : Boolean = false, ct : CellType = TypeInt)(fn : (Int, Int) => Int) : Tile = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    Rasterizer.foreachCellByGeometry(self, re, ie)({ (x,y) => tile.set(x,y,fn(x,y)) })
    tile
  }

  def foreachCellDouble(re : RasterExtent, ie : Boolean = false, ct : CellType = TypeDouble)(fn : (Int, Int) => Double) : Tile = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    Rasterizer.foreachCellByGeometry(self, re, ie)({ (x,y) => tile.setDouble(x,y,fn(x,y)) })
    tile
  }

  def rasterize(re : RasterExtent)(fn : Transformer[Int]) =
    Rasterizer.rasterize(self, re)(fn)
}
