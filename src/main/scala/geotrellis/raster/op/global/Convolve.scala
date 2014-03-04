/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.raster.op.global

import geotrellis._
import geotrellis.raster._
import geotrellis.feature.Point
import geotrellis.feature.op._

/**
 * Computes the convolution of two rasters.
 *
 * @param      r       Raster to convolve.
 * @param      k       Kernel that represents the convolution filter.
 * @param      tns     TileNeighbors that describe the neighboring tiles.
 *
 * @note               Convolve does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class Convolve(r:Op[Raster], k:Op[Kernel]) extends Op2(r,k)({
  (r,kernel) => 
    val convolver = new Convolver(r.rasterExtent,kernel)

    val cols = r.rasterExtent.cols
    val rows = r.rasterExtent.rows

    val kraster = kernel.raster

    val kernelrows = kraster.rows
    val kernelcols = kraster.cols

    var col = 0
    var row = 0

    while(row < rows) {
      col = 0
      while(col < cols) {
        val z = r.get(col,row)
        if (isData(z)) {
          convolver.stampKernel(col,row,z)
        }

        col += 1
      }
      row += 1
    }

    Result(convolver.result)
})
