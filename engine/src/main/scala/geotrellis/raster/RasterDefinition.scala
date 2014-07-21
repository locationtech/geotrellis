/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.raster

import geotrellis.vector.Extent
import geotrellis.engine.LayerId

case class RasterDefinition(layerId: LayerId,
                            rasterExtent: RasterExtent,
                            tileLayout: TileLayout,
                            cellType: CellType,
                            catalogued: Boolean = true) {
  def isTiled = tileLayout.isTiled
  lazy val tileExtents = TileExtents(rasterExtent.extent, tileLayout)

  def withType(newType: CellType) =
    new RasterDefinition(layerId, rasterExtent, tileLayout, newType, catalogued)

  def withRasterExtent(target: RasterExtent) =
    new RasterDefinition(layerId, target, TileLayout.singleTile(target), cellType, catalogued)

  def tileExtent(tileCol: Int, tileRow: Int): Extent = {
    val minCol = tileCol * tileLayout.pixelCols
    val minRow = tileRow * tileLayout.pixelRows
    val grid = GridBounds(
      minCol,
      minRow,
      minCol + tileLayout.pixelCols - 1,
      minRow + tileLayout.pixelRows - 1
    )
    rasterExtent.extentFor(grid)
  }
}

object RasterDefinition {
  def fromRaster(r: Tile, extent: Extent): RasterDefinition = {
    val rasterExtent = RasterExtent(extent, r.cols, r.rows)
    RasterDefinition(
      LayerId.MEM_RASTER,
      rasterExtent,
      TileLayout.singleTile(rasterExtent),
      r.cellType,
      false)
  }
}
