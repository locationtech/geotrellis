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

import spire.syntax.cfor._


/**
  * Extension methods for invoking the rasterizer on
  * [[RasterExtent]]s.
  */
trait RasterExtentRasterizeMethods[T <: RasterExtent] extends MethodExtensions[T] {

  /**
    * Call the function 'fn' on each cell of present [[RasterExtent]]
    * that is covered by the Geometry.  The precise definition of the
    * word "covered" is determined by the options parameter.
    */
  def foreach(
    geom : Geometry,
    options: Options = Options.DEFAULT
  )(fn : (Int, Int) => Unit) : Unit =
    geom.foreach(self, options)(fn)

  /**
    * Call the function 'fn' on each cell of present [[RasterExtent]]
    * that is covered by the Geometry.  The precise definition of the
    * word "covered" is determined by the options parameter.  The
    * result is a new [[Raster]].
    */
  def rasterize(
    geom: Geometry,
    options: Options = Options.DEFAULT,
    ct: CellType = IntConstantNoDataCellType
  )(fn: (Int, Int) => Int): Raster[ArrayTile] =
    geom.rasterize(self, ct, options)(fn)

  /**
    * Call the function 'fn' on each cell of present [[RasterExtent]]
    * that is covered by the Geometry.  The precise definition of the
    * word "covered" is determined by the options parameter.  The
    * result is a new [[Raster]].
    */
  def rasterizeDouble(
    geom: Geometry,
    options: Options = Options.DEFAULT,
    ct: CellType = DoubleConstantNoDataCellType
  )(fn: (Int, Int) => Double): Raster[ArrayTile] =
    geom.rasterizeDouble(self, ct, options)(fn)
}
