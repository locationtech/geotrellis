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

package geotrellis.raster

import geotrellis._

/**
 * Supplies functionaltiy to operations that do convolution.
 */
case class Convolver(rasterExtent:RasterExtent,k:Kernel) {

  val cols = rasterExtent.cols
  val rows = rasterExtent.rows

  val kraster = k.raster
  var kernelcols = kraster.cols
  var kernelrows = kraster.rows

  val data:IntArrayRasterData = IntArrayRasterData.empty(rasterExtent.cols,rasterExtent.rows)

  def stampKernel(col:Int,row:Int,z:Int) = {
    if(z == 0) {
      val o = data.get(col,row)
      data.set(col,row,
        if(isNoData(o)) 0
        else o
      )
    } else {

      val rowmin = row - kernelrows / 2
      val rowmax = math.min(row + kernelrows / 2 + 1, rows)
      
      val colmin = col - kernelcols / 2
      val colmax = math.min(col + kernelcols / 2 + 1, cols)

      var kcol = 0
      var krow = 0

      var r = rowmin
      var c = colmin
      while(r < rowmax) {
        while(c < colmax) {
          if (r >= 0 && c >= 0 && r < rows && c < cols &&
            kcol >= 0 && krow >= 0 && kcol < kernelcols && krow < kernelrows) {

            val k = kraster.get(kcol,krow)
            if (isData(k)) {
              val o = data.get(c,r)
              val w = if (isNoData(o)) {
                k * z
              } else {
                o + k*z
              }
              data.set(c,r,w)
            }
          }

          c += 1
          kcol += 1
        }

        kcol = 0
        c = colmin
        r += 1
        krow += 1
      }
    }
  }

  def result = Raster(data,rasterExtent)
}
