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

package geotrellis.raster.rasterize.extent

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.rasterize.Rasterizer.Options
import geotrellis.vector._

import math.{min, max, round, floor, ceil}


/**
  * Object holding extent rasterization functions.
  */
object ExtentRasterizer {

  /**
    * This function causes the function f to be called on each pixel
    * that interacts with the extent.  The definition of the word
    * "interacts" is controlled by the options parameter.
    *
    * @param e        An extent to render
    * @param re       A raster extent to render into
    * @param options  The options parameter controls whether to treat pixels as points or areas and whether to report partially-intersected areas.
    */
  def foreachCellByExtent(e: Extent, re: RasterExtent, options: Options = Options.DEFAULT)(f: (Int, Int) => Unit): Unit = {
    val xmin = re.mapXToGridDouble(e.xmin)
    val ymin = re.mapYToGridDouble(e.ymax)
    val xmax = re.mapXToGridDouble(e.xmax)
    val ymax = re.mapYToGridDouble(e.ymin)

    val sampleType = options.sampleType
    val partial = options.includePartial

    val (rowMin, rowMax, colMin, colMax) =
      if (sampleType == PixelIsPoint) {(
        max(round(xmin), 0).toInt,
        min(round(xmax), re.cols).toInt,
        max(round(ymin), 0).toInt,
        min(round(ymax), re.rows).toInt
      )}
      else if (partial) {(
        max(floor(xmin), 0).toInt,
        min(ceil(xmax), re.cols).toInt,
        max(floor(ymin), 0).toInt,
        min(ceil(ymax), re.rows).toInt
      )}
      else {(
        max(ceil(xmin), 0).toInt,
        min(floor(xmax), re.cols).toInt,
        max(ceil(ymin), 0).toInt,
        min(floor(ymax), re.rows).toInt
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
