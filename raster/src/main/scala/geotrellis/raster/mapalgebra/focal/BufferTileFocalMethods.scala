/*
 * Copyright 2021 Azavea
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
import geotrellis.util.MethodExtensions

trait BufferTileFocalMethods extends MethodExtensions[BufferTile] {

  /** Computes the minimum value of a neighborhood */
  def focalMin(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalMin(n, bounds, target))

  /** Computes the maximum value of a neighborhood */
  def focalMax(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalMax(n, bounds, target))

  /** Computes the mode of a neighborhood */
  def focalMode(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalMode(n, bounds, target))

  /** Computes the median of a neighborhood */
  def focalMedian(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalMedian(n, bounds, target))

  /** Computes the mean of a neighborhood */
  def focalMean(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalMean(n, bounds, target))

  /** Computes the sum of a neighborhood */
  def focalSum(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalSum(n, bounds, target))

  /** Computes the standard deviation of a neighborhood */
  def focalStandardDeviation(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.focalStandardDeviation(n, bounds, target))

  /** Computes the next step of Conway's Game of Life */
  def focalConway(bounds: Option[GridBounds[Int]] = None): BufferTile =
    self.mapTile(_.focalConway(bounds))

  /** Computes the convolution of the raster for the given kernl */
  def convolve(kernel: Kernel, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.convolve(kernel, bounds, target))

  /**
   * Calculates spatial autocorrelation of cells based on the
   * similarity to neighboring values.
   */
  def tileMoransI(n: Neighborhood, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.tileMoransI(n, bounds, target))

  /**
   * Calculates global spatial autocorrelation of a raster based on
   * the similarity to neighboring values.
   */
  def scalarMoransI(n: Neighborhood, bounds: Option[GridBounds[Int]] = None): Double =
    self.sourceTile.scalarMoransI(n, bounds)

  /**
   * Calculates the slope of each cell in a raster.
   *
   * @param   cs         cellSize of the raster
   * @param   zFactor    Number of map units to one elevation unit.
   */
  def slope(cs: CellSize, zFactor: Double = 1.0, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.slope(cs, zFactor, bounds, target))

  /**
   * Calculates the aspect of each cell in a raster.
   *
   * @param   cs          cellSize of the raster
   */
  def aspect(cs: CellSize, bounds: Option[GridBounds[Int]] = None, target: TargetCell = TargetCell.All): BufferTile =
    self.mapTile(_.aspect(cs, bounds, target))
}
