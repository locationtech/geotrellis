/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster.op.global

import geotrellis.raster._
import geotrellis.feature.Point

import spire.syntax.cfor._

/**
 * Computes the convolution of two rasters.
 *
 * @param      r       Tile to convolve.
 * @param      k       Kernel that represents the convolution filter.
 *
 * @note               Convolve does not currently support Double raster data.
 *                     If you use a Tile with a Double CellType (TypeFloat, TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Convolve {
  def apply(t: Tile, kernel: Kernel): Tile = {
    val cols = t.cols
    val rows = t.rows

    val convolver = new Convolver(cols, rows, kernel)

    cfor(0)(_ < rows, _ + 1) { row =>
      cfor(0)(_ < cols, _ + 1) { col =>
        val z = t.get(col, row)
        if (isData(z)) {
          convolver.stampKernel(col, row, z)
        }
      }
    }

    convolver.result
  }
}
