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
  * Takes the most common value in a region and resamples to that.
  *
  * As with other aggregate resampling methods, this is most useful when
  * decreasing resolution or downsampling.
  *
  */
class ModeResample(tile: Tile, extent: Extent, targetCS: CellSize)
    extends AggregateResample(tile, extent, targetCS) {

  private def calculateIntMode(indices: Seq[(Int, Int)]): Int = {
    val values = indices.foldLeft(mutable.Map[Int, Int]()) { (hash, coords) =>
      val v = tile.get(coords._1, coords._2)
      hash(v) = hash.getOrElseUpdate(v, 0) + 1
      hash
    }.toSeq
    if (values.size > 0) values.maxBy { case (key, value) => value }._1
    else NODATA
  }

  private def calculateDoubleMode(indices: Seq[(Int, Int)]): Double = {
    val values = indices.foldLeft(mutable.Map[Double, Int]()) { (hash, coords) =>
      val v = tile.getDouble(coords._1, coords._2)
      hash(v) = hash.getOrElseUpdate(v, 0) + 1
      hash
    }.toSeq
    if (values.size > 0) values.maxBy { case (key, value) => value }._1
    else Double.NaN
  }

  def resampleValid(x: Double, y: Double): Int =
    calculateIntMode(contributions(x, y))

  def resampleDoubleValid(x: Double, y: Double): Double =
    calculateDoubleMode(contributions(x, y))
}
