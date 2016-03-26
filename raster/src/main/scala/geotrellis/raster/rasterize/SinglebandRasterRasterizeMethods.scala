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
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.Geometry


/**
  * Extension methods for [[Raster]]s.
  */
trait SinglebandRasterRasterizeMethods[T <: Tile] extends MethodExtensions[Raster[T]] {

  /**
    * Call the function 'fn' on each cell of present [[Raster]] that
    * is covered by the [[Geometry]].  The precise definition of the
    * word "covered" is determined by the options parameter.
    */
  def foreachCell(
    geom : Geometry,
    options: Options = Options.DEFAULT
  )(fn : Int => Int) : Tile = {
    val extent = self.extent
    val re = RasterExtent(extent, self.cols, self.rows)
    val tile = self.tile
    val ct = tile.cellType
    val mutableTile =
      if (!ct.isFloatingPoint) ArrayTile.empty(tile.cellType, re.cols, re.rows)
      else IntArrayTile.empty(re.cols, re.rows)

    Rasterizer.foreachCellByGeometry(geom, re, options)(new Callback {
      def apply(col : Int, row : Int): Unit = {
        val z = tile.get(col, row)
        mutableTile.set(col, row, fn(z))
      }
    })
    mutableTile
  }

  /**
    * Call the function 'fn' on each cell of present [[Raster]] that
    * is covered by the [[Geometry]].  The precise definition of the
    * word "covered" is determined by the options parameter.
    */
  def foreachCellDouble(
    geom : Geometry,
    options: Options = Options.DEFAULT
  )(fn : Double => Double) : Tile = {
    val extent = self.extent
    val re = RasterExtent(extent, self.cols, self.rows)
    val tile = self.tile
    val ct = tile.cellType
    val mutableTile =
      if (ct.isFloatingPoint) ArrayTile.empty(tile.cellType, re.cols, re.rows)
      else DoubleArrayTile.empty(re.cols, re.rows)

    Rasterizer.foreachCellByGeometry(geom, re, options)(new Callback {
      def apply(col : Int, row : Int): Unit = {
        val z = tile.get(col, row)
        mutableTile.setDouble(col, row, fn(z))
      }
    })
    mutableTile
  }
}
