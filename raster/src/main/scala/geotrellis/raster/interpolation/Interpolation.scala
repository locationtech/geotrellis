/*
* Copyright (c) 2015 Azavea.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package geotrellis.raster.interpolation

import geotrellis.raster.{DoubleArrayTile, RasterExtent, Tile}
import geotrellis.vector.Extent
import geotrellis.vector.interpolation.Kriging
import spire.syntax.cfor._

object Interpolation {

  /**
   * Interpolation for a Tile
   * @param tile        Tile to be interpolated
   * @return            Tile set with the interpolated values
   */
  def apply(tile: Tile, extent: Extent)(predictor: (Double, Double) => Double): Tile = {
    val rasterExtent = RasterExtent(tile, extent)
    val result = DoubleArrayTile.empty(tile.cols, tile.rows)

    cfor(0)(_ < tile.cols, _ + 1) { col: Int =>
      cfor(0)(_ < tile.rows, _ + 1) { row: Int =>
        val (x, y) = rasterExtent.gridToMap(col, row)
        val prediction: Double = predictor(x, y)
        result.setDouble(col, row, prediction)
      }
    }

    result
  }

  def kriging(tile: Tile, extent: Extent)(kriging: Kriging): Tile =
    apply(tile, extent) { (x: Double, y: Double) => kriging(x, y)._1 }
}
