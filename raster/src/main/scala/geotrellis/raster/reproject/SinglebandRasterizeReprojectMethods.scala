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

package geotrellis.raster.reproject

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.prototype._
import geotrellis.raster.rasterize._
import geotrellis.raster.resample._
import geotrellis.vector._
import geotrellis.vector.reproject._

trait SinglebandRasterRasterizeReprojectMethods extends RasterRasterizeReprojectMethods[Tile] {

  def rasterizeReproject(
    srcCRS: CRS,
    destRegion: Polygon,
    destRE: RasterExtent, 
    destCRS: CRS,
    resampleMethod: ResampleMethod, 
    destCellType: CellType
  ): ProjectedRaster[Tile] = {
    val buffer = self.tile.prototype(destCellType, destRE.cols, destRE.rows)
    val trans = Proj4Transform(destCRS, srcCRS)
    val resampler = Resample.apply(resampleMethod, self.tile, self.extent, CellSize(self.rasterExtent.cellwidth, self.rasterExtent.cellheight))

    if (self.cellType.isFloatingPoint) {
      Rasterizer.foreachCellByPolygon(destRegion, destRE) { (px, py) =>
        val (x, y) = destRE.gridToMap(px, py)
        val (tx, ty) = trans(x, y)
        buffer.setDouble(px, py, resampler.resampleDouble(tx, ty))
      }
    } else {
      Rasterizer.foreachCellByPolygon(destRegion, destRE) { (px, py) =>
        val (x, y) = destRE.gridToMap(px, py)
        val (tx, ty) = trans(x, y)
        buffer.set(px, py, resampler.resample(tx, ty))
      }
    }

    ProjectedRaster(buffer, destRE.extent, destCRS)
  }

}
