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

package geotrellis.spark.streaming.mapalgebra.focal

import geotrellis.raster.mapalgebra.focal._

trait FocalTileLayerDStreamMethods[K] extends DStreamFocalOperation[K] {

  def focalSum(n: Neighborhood) = focal(n) { (tile, bounds) => Sum(tile, n, bounds) }
  def focalMin(n: Neighborhood) = focal(n) { (tile, bounds) => Min(tile, n, bounds) }
  def focalMax(n: Neighborhood) = focal(n) { (tile, bounds) => Max(tile, n, bounds) }
  def focalMean(n: Neighborhood) = focal(n) { (tile, bounds) => Mean(tile, n, bounds) }
  def focalMedian(n: Neighborhood) = focal(n) { (tile, bounds) => Median(tile, n, bounds) }
  def focalMode(n: Neighborhood) = focal(n) { (tile, bounds) => Mode(tile, n, bounds) }
  def focalStandardDeviation(n: Neighborhood) = focal(n) { (tile, bounds) => StandardDeviation(tile, n, bounds) }
  def focalConway() = { val n = Square(1) ; focal(n) { (tile, bounds) => Sum(tile, n, bounds) } }
  def focalConvolve(k: Kernel) = { focal(k) { (tile, bounds) => Convolve(tile, k, bounds) } }

  def aspect() = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Aspect(tile, n, bounds, cellSize)
    }
  }

  def slope(zFactor: Double = 1.0) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Slope(tile, n, bounds, cellSize, zFactor)
    }
  }
}
