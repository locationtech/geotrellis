/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.mapalgebra.focal.tobler

import geotrellis.raster._
import geotrellis.spark.mapalgebra.focal._
import geotrellis.raster.mapalgebra.focal.tobler._
import geotrellis.raster.mapalgebra.focal._

trait ToblerTileLayerRDDMethods[K] extends FocalOperation[K] {
  /** Calculates the hillshade of each cell in a raster.
   *
   * @see [[geotrellis.raster.mapalgebra.focal.Hillshade]]
   */
  def tobler(zFactor: Double = 1.0, target: TargetCell = TargetCell.All) = {
    val n = Square(1)
    focalWithCellSize(n) { (tile, bounds, cellSize) =>
      Tobler(tile, n, bounds, cellSize, zFactor, target)
    }.mapContext(_.copy(cellType = DoubleConstantNoDataCellType))
  }
}
