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

package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

class BilinearResample(tile: Tile, extent: Extent)
    extends Resample(tile, extent) {

  private val xmin = extent.xmin + cellwidth / 2.0
  private val xmax = extent.xmax - cellwidth / 2.0
  private val ymin = extent.ymin + cellheight / 2.0
  private val ymax = extent.ymax - cellheight / 2.0

  override def resampleValid(x: Double, y: Double): Int = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    bilinearInt(leftCol, topRow, xRatio, yRatio)
  }

  override def resampleDoubleValid(x: Double, y: Double): Double = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    bilinearDouble(leftCol, topRow, xRatio, yRatio)
  }

  def resolveTopLeftCoordsAndRatios(x: Double, y: Double): (Int, Int, Double, Double) = {
    val dleft = x - xmin
    val dright = xmax - x
    val dtop = ymax - y
    val dbottom = y - ymin

    val leftCol = math.floor(dleft / cellwidth).toInt
    val topRow = math.floor(dtop / cellheight).toInt

    val xRatio =
      if (dleft < 0) 1
      else if (dright < 0) 0
      else dleft / cellwidth - leftCol
    val yRatio =
      if (dtop < 0) 1
      else if (dbottom < 0) 0
      else dtop / cellheight - topRow

    (leftCol, topRow, xRatio, yRatio)
  }

  def bilinearInt(leftCol: Int, topRow: Int, xRatio: Double, yRatio: Double): Int = {
    val v = bilinear(leftCol, topRow, xRatio, yRatio)
    if(isData(v)) { math.round(v).toInt }
    else NODATA
  }

  def bilinearDouble(leftCol: Int, topRow: Int, xRatio: Double, yRatio: Double): Double =
    bilinear(leftCol, topRow, xRatio, yRatio)

  private def bilinear(
    leftCol: Int,
    topRow: Int,
    xRatio: Double,
    yRatio: Double): Double = {
    val (rightCol, bottomRow) = (leftCol + 1, topRow + 1)
    val (invXR, invYR) = (1 - xRatio, 1 - yRatio)

    var accum = 0.0
    var accumDivisor = 0.0

    if (leftCol >= 0 && topRow >= 0 && leftCol < cols && topRow < rows) {
      val z = tile.getDouble(leftCol, topRow)
      if(isData(z)) {
        val mult = invXR * invYR
        accumDivisor += mult
        accum += z * mult
      }
    }

    if (rightCol >= 0 && topRow >= 0 && rightCol < cols && topRow < rows) {
      val z = tile.getDouble(rightCol, topRow)
      if(isData(z)) {
        val mult = (1 - invXR) * invYR
        accumDivisor += mult
        accum += z * mult
      }
    }

    if (leftCol >= 0 && bottomRow >= 0 && leftCol < cols && bottomRow < rows) {
      val z = tile.getDouble(leftCol, bottomRow)
      if(isData(z)) {
        val mult = invXR * (1 - invYR)
        accumDivisor += mult
        accum += z * mult
      }
    }

    if (rightCol >= 0 && bottomRow >= 0 && rightCol < cols && bottomRow < rows) {
      val z = tile.getDouble(rightCol, bottomRow)
      if(isData(z)) {
        val mult = (1 - invXR) * (1 - invYR)
        accumDivisor += mult
        accum += z * mult
      }
    }

    accum / accumDivisor
  }

}
