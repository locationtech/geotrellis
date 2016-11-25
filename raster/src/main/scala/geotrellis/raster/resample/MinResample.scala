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

import scala.math
import spire.syntax.cfor._

/**
  * Takes the average value for all cells within the index boundaries provided by
  * inheritance from AggregateResample
  */
class MinResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateMin(indices: Seq[(Int, Int)]): Int =
    indices.foldLeft(Int.MaxValue) { case (currentMin, coords) =>
      math.min(currentMin, tile.get(coords._1, coords._2))
    }

  private def calculateMinDouble(indices: Seq[(Int, Int)]): Double = {
    val doubleMin = indices.foldLeft(Double.MaxValue) { case (currentMin, coords) =>
      val v = tile.getDouble(coords._1, coords._2)
      // Double.NaN would *always* be max
      if (isData(v)) math.min(currentMin, v) else currentMin
    }
    if (doubleMin == Double.MaxValue) NODATA else doubleMin
  }

  def resampleValid(x: Double, y: Double): Int = calculateMin(contributions(x, y))

  def resampleDoubleValid(x: Double, y: Double): Double = calculateMinDouble(contributions(x,y))

}
