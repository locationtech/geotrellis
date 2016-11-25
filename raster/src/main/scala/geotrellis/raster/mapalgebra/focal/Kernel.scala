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

package geotrellis.raster.mapalgebra.focal

import geotrellis.raster._
import geotrellis.vector.Extent

/**
  * [[Kernel]]
  *
  * Represents a neighborhood that is represented by a tile.
  */
case class Kernel(tile: Tile) extends Neighborhood {
  require(tile.rows == tile.cols, "Kernel tile must be square")
  require(tile.rows % 2 == 1, "Kernel tile must have odd dimension")
  val extent = (tile.rows / 2).toInt

  // Not supporting masks, since masks are implemented as 0 values in the kernel weight
  val hasMask = false

  def cellType = tile.cellType
}

object Kernel {
  implicit def tile2Kernel(r: Tile): Kernel = Kernel(r)

  def apply(nbhd: Neighborhood): Kernel = {
    require(nbhd.hasMask, "Neighborhood must have a mask method")

    val w = 2 * nbhd.extent + 1
    val size = w * w
    val tile = IntArrayTile(new Array[Int](size), w, w)

    var r = 0
    while (r < w) {
      var c = 0
      while (c < w) {
        tile.set(c, r, if (nbhd.mask(c, r)) 0 else 1)
        c = c + 1
      }
      r = r + 1
    }

    new Kernel(tile)
  }

  /**
    * Creates a Gaussian kernel. Can be used with the [[Convolve]] or
    * KernelDensity operations.
    *
    * @param    size           Number of rows of the resulting tile.
    * @param    sigma          Sigma parameter for Gaussian
    * @param    amp            Amplitude for Gaussian. Will be the value at the center of
    *                          the resulting tile.
    *
    * @note                    Tile will be IntConstantNoDataCellType
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
    * Creates a Circle kernel. Can be used with the [[Convolve]] or
    * KernelDensity operations.
    *
    * @param       size           Number of rows in the resulting tile.
    * @param       cellWidth      Cell width of the resutling tile.
    * @param       rad            Radius of the circle.
    *
    * @note                       Tile will be IntConstantNoDataCellType
    */
  def circle(size: Int, cellWidth: Double, rad: Int) = {
    val output = IntArrayTile.empty(size, size)

    val rad2 = rad*rad

    val w = size/2
    var r = -w
    var c = -w
    while(r <= w) {
      while(c <= w) {
        output.set(c+w, r+w, if (r * r + c * c <= rad2) 1 else 0)
        c += 1
      }
      c = -w
      r += 1
    }

    Kernel(output)
  }
}
