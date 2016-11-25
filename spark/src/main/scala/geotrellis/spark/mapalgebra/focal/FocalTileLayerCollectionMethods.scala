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

package geotrellis.spark.mapalgebra.focal

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal._

trait FocalTileLayerCollectionMethods[K] extends CollectionFocalOperation[K] {

  def focalSum(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Sum(tile, n, bounds, target) }
  def focalMin(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Min(tile, n, bounds, target) }
  def focalMax(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Max(tile, n, bounds, target) }
  def focalMean(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Mean(tile, n, bounds, target) }
  def focalMedian(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Median(tile, n, bounds, target) }
  def focalMode(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => Mode(tile, n, bounds, target) }
  def focalStandardDeviation(n: Neighborhood, target: TargetCell = TargetCell.All) = focal(n) { (tile, bounds) => StandardDeviation(tile, n, bounds, target) }
  def focalConway() = { val n = Square(1) ; focal(n) { (tile, bounds) => Sum(tile, n, bounds) } }
  def focalConvolve(k: Kernel, target: TargetCell = TargetCell.All) = { focal(k) { (tile, bounds) => Convolve(tile, k, bounds, target) } }

  /** Calculates the aspect of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Aspect]]
   */
  def aspect(target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Aspect(tile, n, bounds, cellSize, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }

  /** Calculates the slope of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Slope]]
   */
  def slope(zFactor: Double = 1.0, target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Slope(tile, n, bounds, cellSize, zFactor, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
