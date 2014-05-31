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

package geotrellis.raster

import geotrellis._
import geotrellis.feature.Extent

/** 
 * Kernel
 *
 * Represents a neighborhood that is represented by
 * a raster.
 */
case class Kernel(raster: Tile) {
  if(raster.rows != raster.cols) sys.error("Kernel raster must be square")
  if(raster.rows % 2 != 1) sys.error("Kernel raster must have odd dimension")
}

object Kernel {
  implicit def raster2Kernel(r: Tile): Kernel = Kernel(r)
  
  /**
   * Creates a Gaussian kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param    size           Number of rows of the resulting raster.
   * @param    sigma          Sigma parameter for Gaussian
   * @param    amp            Amplitude for Gaussian. Will be the value at the center of
   *                          the resulting raster.
   *
   * @note                    Tile will be TypeInt
   */
  def gaussian(size: Int, sigma: Double, amp: Double): Kernel = {
    val output = IntArrayTile.empty(size, size)

    val denom = 2.0*sigma*sigma

    var r = 0
    var c = 0
    while(r < size) {
      c = 0
      while(c < size) {
        val rsqr = (c - (size / 2)) * (c - (size / 2)) + (r - (size / 2)) * (r - (size / 2))
        val g = (amp * (math.exp(-rsqr / denom))).toInt
        output.set(c, r, g)
        c += 1
      }
      r += 1
    }

    Kernel(output)
  }

  /**
   * Creates a Circle kernel. Can be used with the [[Convolve]] or [[KernelDensity]] operations.
   *
   * @param       size           Number of rows in the resulting raster.
   * @param       cellWidth      Cell width of the resutling raster.
   * @param       rad            Radius of the circle.
   *
   * @note                       Tile will be TypeInt 
   */
  def circle(size: Int, cellWidth: Double, rad: Int) = {
    val output = IntArrayTile.empty(size, size)

    val rad2 = rad*rad

    var r = 0
    var c = 0
    while(r < size) {
      while(c < size) {
        output.set(c, r, if (r * r + c * c < rad2) 1 else 0)
        c += 1
      }
      c = 0
      r += 1
    }

    Kernel(output)
  }
}
