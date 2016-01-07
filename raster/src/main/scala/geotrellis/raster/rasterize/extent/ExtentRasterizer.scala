/*
 * Copyright (c) 2015 Azavea.
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

package geotrellis.raster.rasterize.extent

import geotrellis.raster._
import geotrellis.vector._
import geotrellis.raster.rasterize._

import math.{min, max, round, floor, ceil}

object ExtentRasterizer {

  /**
   * This function causes the function f to be called on each pixel
   * "inside" of the given extent.  The definition of the word
   * "inside" depends on the includeExterior.
   *
   * @param e                An extent to render
   * @param re               A raster extent to render into
   * @param includeExterior  If true, report pixels that intersect the extent, otherwise report pixels whose centers are in the extent
   */
  def foreachCellByExtent(e: Extent, re: RasterExtent, includeExterior: Boolean = false)(f: (Int, Int) => Unit): Unit = {
    val xmin = re.mapXToGridDouble(e.xmin)
    val ymin = re.mapYToGridDouble(e.ymax)
    val xmax = re.mapXToGridDouble(e.xmax)
    val ymax = re.mapYToGridDouble(e.ymin)

    val (rowMin, rowMax, colMin, colMax) =
      if (!includeExterior) {(
        max(round(xmin), 0).toInt,
        min(round(xmax), re.cols).toInt,
        max(round(ymin), 0).toInt,
        min(round(ymax), re.rows).toInt
      )}
      else {(
        max(floor(xmin), 0).toInt,
        min(ceil(xmax), re.cols).toInt,
        max(floor(ymin), 0).toInt,
        min(ceil(ymax), re.rows).toInt
      )}

    var x = rowMin
    while (x < rowMax) {
      var y = colMin
      while (y < colMax) {
        f(x,y)
        y += 1
      }
      x += 1
    }
  }

}
