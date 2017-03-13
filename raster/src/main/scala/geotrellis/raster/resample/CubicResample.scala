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

import spire.syntax.cfor._

/**
  * This abstract class serves as a base class for the family of
  * cubic resample algorithms implemented. As a constructor argument
  * it takes the dimension of the cube. It takes the closest dimension ^ 2
  * points and then resamples over those points.
  *
  * If there is less then dimension ^ 2 points obtainable for the
  * current point the implementation extrapolates points in order to
  * approximate the derivatives that it needs.
  *
  * Note that this class is single-threaded.
  */
abstract class CubicResample(tile: Tile, extent: Extent, dimension: Int)
    extends BilinearResample(tile, extent) {

  private val cubicTile =
    ArrayTile(Array.ofDim[Double](dimension * dimension), dimension, dimension)

  protected def cubicResample(
    t: Tile,
    x: Double,
    y: Double): Double

  private def setCubicValues(leftCol: Int, topRow: Int, f: (Int, Int) => Double) = {
    val offset = dimension / 2

    cfor(0)(_ < dimension, _ + 1) { i =>
      cfor(0)(_ < dimension, _ + 1) { j =>
        val v = f(leftCol - offset + 1 + j, topRow - offset + 1 + i)
        cubicTile.setDouble(j, i, v)
      }
    }
  }

  private def getter(col: Int, row: Int) = {
    if ((col >= cols) && (row >= rows)) { // lower-right
      tile.get(cols-1, rows-1)
    }
    else if ((col >= cols) && (row < 0)) { //upper-right
      tile.get(cols-1, 0)
    }
    else if ((col < 0) && (row >= rows)) { // lower-left
      tile.get(0, rows-1)
    }
    else if ((col < 0) && (row < 0)) { // upper-left
      tile.get(0, 0)
    }
    else if (col < 0) { // left
      tile.get(0, row)
    }
    else if (col >= cols) { // right
      tile.get(cols-1, row)
    }
    else if (row < 0) { // top
      tile.get(col, 0)
    }
    else if (row >= rows) { // bottom
      tile.get(col, rows-1)
    }
    else
      tile.get(col, row)
  }

  private def getterDouble(col: Int, row: Int) = {
    if ((col >= cols) && (row >= rows)) { // lower-right
      tile.getDouble(cols-1, rows-1)
    }
    else if ((col >= cols) && (row < 0)) { //upper-right
      tile.getDouble(cols-1, 0)
    }
    else if ((col < 0) && (row >= rows)) { // lower-left
      tile.getDouble(0, rows-1)
    }
    else if ((col < 0) && (row < 0)) { // upper-left
      tile.getDouble(0, 0)
    }
    else if (col < 0) { // left
      tile.getDouble(0, row)
    }
    else if (col >= cols) { // right
      tile.getDouble(cols-1, row)
    }
    else if (row < 0) { // top
      tile.getDouble(col, 0)
    }
    else if (row >= rows) { // bottom
      tile.getDouble(col, rows-1)
    }
    else
      tile.getDouble(col, row)
  }

  override def resampleValid(x: Double, y: Double): Int = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    setCubicValues(leftCol, topRow, getter)
    cubicResample(cubicTile, xRatio, yRatio).round.toInt
  }

  override def resampleDoubleValid(x: Double, y: Double): Double = {
    val (leftCol, topRow, xRatio, yRatio) = resolveTopLeftCoordsAndRatios(x, y)
    setCubicValues(leftCol, topRow, getterDouble)
    cubicResample(cubicTile, xRatio, yRatio)
  }

}
