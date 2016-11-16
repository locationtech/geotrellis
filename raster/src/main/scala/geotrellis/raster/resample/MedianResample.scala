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
import geotrellis.raster.summary._
import geotrellis.raster.histogram._
import geotrellis.vector.Extent

import scala.collection.mutable
import collection._

import spire.syntax.cfor._

/**
  * Takes the ordinally-middlemost value in a region and resamples to that.
  *
  * As with other aggregate resampling methods, this is most useful when
  * decreasing resolution or downsampling.
  *
  */
class MedianResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateIntMedian(indices: Seq[(Int, Int)]): Int = {
    val contributions = indices.map { case (x, y) => tile.get(x, y) }.sorted
    val mid: Int = contributions.size / 2

    if (contributions.isEmpty) NODATA
    else if (contributions.size % 2 == 0) (contributions(mid - 1) + contributions(mid)) / 2
    else contributions(mid)
  }

  private def calculateDoubleMedian(indices: Seq[(Int, Int)]): Double = {
    val contributions = indices.map { case (x, y) => tile.getDouble(x, y) }.sorted
    val mid: Int = contributions.size / 2

    if (contributions.isEmpty) Double.NaN
    else if (contributions.size % 2 == 0) (contributions(mid - 1) + contributions(mid)) / 2
    else contributions(mid)
  }

  override def resampleValid(x: Double, y: Double): Int =
    calculateIntMedian(contributions(x, y))

  override def resampleDoubleValid(x: Double, y: Double): Double =
    calculateDoubleMedian(contributions(x, y))

}
