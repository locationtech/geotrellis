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

package geotrellis.spark.mapalgebra.focal.hillshade

import geotrellis.raster._
import geotrellis.raster.Neighborhoods.Square
import geotrellis.raster.mapalgebra.focal.hillshade._
import geotrellis.spark._
import geotrellis.spark.mapalgebra.focal.FocalOperation

import org.apache.spark.Partitioner


trait HillshadeTileLayerRDDMethods[K] extends FocalOperation[K] {
  /** Calculates the hillshade of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Hillshade]]
   */
  def hillshade(
    azimuth: Double = 315,
    altitude: Double = 45,
    zFactor: Double = 1,
    target: TargetCell = TargetCell.All,
    partitioner: Option[Partitioner] = None
  ) = {
    val n = Square(1)
    focalWithCellSize(n, partitioner) { (tile, bounds, cellSize) =>
      Hillshade(tile, n, bounds, cellSize, azimuth, altitude, zFactor, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
