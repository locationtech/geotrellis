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
import geotrellis.util.MethodExtensions
import geotrellis.vector.Geometry

import spire.syntax.cfor._


trait RasterExtentRasterizeMethods[T <: RasterExtent] extends MethodExtensions[T] {

  def foreachCell(
    geom : Geometry,
    options: Options = Options.DEFAULT,
    ct : CellType = IntConstantNoDataCellType
  )(fn : (Int, Int) => Unit) : Unit =
    geom.foreachCell(self, options)(fn)

  def foreachCell(fn: (Int, Int) => Unit): Unit = {
    val cols = self.cols
    val rows = self.rows

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        fn(col, row)
      }
    }
  }

  def rasterize(
    geom : Geometry,
    options: Options = Options.DEFAULT,
    ct : CellType = IntConstantNoDataCellType
  )(fn : (Int, Int) => Int) : Tile =
    geom.rasterize(self, options, ct)(fn)

  def rasterizeDouble(
    geom : Geometry,
    options: Options = Options.DEFAULT,
    ct : CellType = DoubleConstantNoDataCellType
  )(fn : (Int, Int) => Double) : Tile =
    geom.rasterizeDouble(self, options, ct)(fn)
}
