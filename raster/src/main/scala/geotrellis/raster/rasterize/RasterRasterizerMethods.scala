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
import geotrellis.util.MethodExtensions

import spire.syntax.cfor._


trait RasterRasterizeMethods[+T <: Tile] extends MethodExtensions[Raster[T]] {

  def foreach(fn: Int => Unit): Unit =
    self.tile.foreach(fn)

  def foreachDouble(fn: Double => Unit): Unit =
    self.tile.foreachDouble(fn)

  def foreach(fn: (Int, Int, Int) => Unit): Unit = {
    val tile = self.tile
    val cols = tile.cols
    val rows = tile.rows

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        fn(col, row, tile.get(col, row))
      }
    }
  }

  def foreachDouble(fn: (Int, Int, Double) => Unit): Unit = {
    val tile = self.tile
    val cols = tile.cols
    val rows = tile.rows

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        fn(col, row, tile.getDouble(col, row))
      }
    }
  }
}
