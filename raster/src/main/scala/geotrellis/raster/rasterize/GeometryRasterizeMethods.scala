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

package geotrellis.raster.rasterize

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.util.MethodExtensions
import geotrellis.vector.Geometry


/**
  * Extension methods for invoking the rasterizer on Geometry objects.
  */
trait GeometryRasterizeMethods extends MethodExtensions[Geometry] {

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present Geometry.  The precise definition
    * of the word "covered" is determined by the options parameter.
    */
  def foreach(
    re : RasterExtent,
    options: Options = Options.DEFAULT
  )(fn : (Int, Int) => Unit) : Unit =
    Rasterizer.foreachCellByGeometry(self, re, options)(fn)

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present Geometry.  The precise definition
    * of the word "covered" is determined by the options parameter.
    * The result is a [[Raster]].
    */
  def rasterize(
    re : RasterExtent,
    ct : CellType = IntConstantNoDataCellType,
    options: Options = Options.DEFAULT
  )(fn : (Int, Int) => Int) : Raster[ArrayTile] = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    val extent = re.extent

    Rasterizer.foreachCellByGeometry(self, re, options)({ (x,y) => tile.set(x,y,fn(x,y)) })
    Raster(tile, extent)
  }
  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present Geometry.  The result is a [[Raster]].
    */
  def rasterizeWithValue(
    re: RasterExtent,
    value: Int,
    ct : CellType = IntConstantNoDataCellType,
    options: Options = Options.DEFAULT
  ): Raster[ArrayTile] =
    rasterize(re, ct, options)({ (col: Int, row: Int) => value })

  /**
    * Call the function 'fn' on each cell of given [[RasterExtent]]
    * that is covered by the present Geometry.  The precise definition
    * of the word "covered" is determined by the options parameter.
    * The result is a [[Raster]].
    */
  def rasterizeDouble(
    re : RasterExtent,
    ct : CellType = DoubleConstantNoDataCellType,
    options: Options = Options.DEFAULT
  )(fn : (Int, Int) => Double) : Raster[ArrayTile] = {
    val tile = ArrayTile.empty(ct, re.cols, re.rows)
    val extent = re.extent

    Rasterizer.foreachCellByGeometry(self, re, options)({ (x,y) => tile.setDouble(x,y,fn(x,y)) })
    Raster(tile, extent)
  }

  /**
    * Fill in 'value' at each cell of given [[RasterExtent]] that is
    * covered by the present Geometry.  The result is a [[Raster]].
    */
  def rasterizeWithValueDouble(
    re: RasterExtent,
    value: Double,
    ct : CellType = DoubleConstantNoDataCellType,
    options: Options = Options.DEFAULT
  ): Raster[ArrayTile] =
    rasterizeDouble(re, ct, options)({ (col: Int, row: Int) => value })
}
