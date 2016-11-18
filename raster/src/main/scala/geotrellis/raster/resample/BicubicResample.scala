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
  * All classes inheriting from this class uses the resample as follows:
  * First the 4 rows each containing 4 points are
  * resampled, then each result is stored and together resampled.
  *
  * If there can't be 16 points resolved, it falls back to bilinear
  * resampling.
  */
abstract class BicubicResample(tile: Tile, extent: Extent, dimension: Int)
    extends CubicResample(tile, extent, dimension) {

  private val columnResults = Array.ofDim[Double](dimension)

  private val iterArray = Array.ofDim[Double](dimension)

  protected def uniCubicResample(p: Array[Double], v: Double): Double

  override def cubicResample(
    t: Tile,
    x: Double,
    y: Double): Double = {

    cfor(0)(_ < dimension, _ + 1) { i =>
      cfor(0)(_ < dimension, _ + 1) { j =>
        iterArray(j) = t.getDouble(j, i)
      }

      columnResults(i) = uniCubicResample(iterArray, x)
    }

    uniCubicResample(columnResults, y)
  }

}
