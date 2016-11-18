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
  * Takes the average value for all cells within the index boundaries provided by
  * inheritance from AggregateResample
  */
class AverageResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateAverage(indices: Seq[(Int, Int)]): Double = {
    val (sum, count) =
      indices.foldLeft((0.0, 0)) { case ((sum, count), coords) =>
        val v = tile.getDouble(coords._1, coords._2)
        if (isData(v)) (sum + v, count + 1)
        else (sum, count)
      }
    if (count > 0) (sum / count) else Double.NaN
  }

  def resampleValid(x: Double, y: Double): Int = {
    val doubleAvg = calculateAverage(contributions(x, y))
    if (isData(doubleAvg)) doubleAvg.toInt else Int.MinValue
  }

  def resampleDoubleValid(x: Double, y: Double): Double = {
    calculateAverage(contributions(x, y))
  }

}
