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

trait SinglebandRasterResampleMethods extends RasterResampleMethods[SinglebandRaster] {
  def resample(target: RasterExtent, method: ResampleMethod): SinglebandRaster = {
    val (cols, rows) = (target.cols, target.rows)
    val targetTile = ArrayTile.empty(self.cellType, cols, rows)
    val targetCS = CellSize(self.extent, cols, rows)
    val resampler = Resample(method, self.tile, self.extent, targetCS)

    if(targetTile.cellType.isFloatingPoint) {
      val interpolate = resampler.resampleDouble _
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val x = target.gridColToMap(col)
          val y = target.gridRowToMap(row)
          val v = interpolate(x, y)
          targetTile.setDouble(col, row, v)
        }
      }
    } else {
      val interpolate = resampler.resample _
      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val x = target.gridColToMap(col)
          val y = target.gridRowToMap(row)
          val v = interpolate(x, y)
          targetTile.set(col, row, v)
        }
      }
    }

    Raster(targetTile, target.extent)
  }
}
