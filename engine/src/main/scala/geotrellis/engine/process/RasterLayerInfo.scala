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

package geotrellis.process

import geotrellis._
import geotrellis.raster._
import geotrellis.source.RasterDefinition

case class RasterLayerInfo(id: LayerId,
                           cellType: CellType,
                           rasterExtent: RasterExtent,
                           epsg: Int,
                           xskew: Double,
                           yskew: Double,
			   tileLayout: TileLayout,
                           shouldCache: Boolean = false) {
  var cached = false
}


object RasterLayerInfo {
  //* For untiled rasters */
  def apply(id: LayerId,
            cellType: CellType,
            rasterExtent: RasterExtent,
            epsg: Int,
            xskew: Double,
            yskew: Double): RasterLayerInfo = {
    val tl = TileLayout(1, 1, rasterExtent.cols, rasterExtent.rows)
    RasterLayerInfo(id, cellType, rasterExtent, epsg, xskew, yskew, false)
  }

  def apply(id: LayerId,
            cellType: CellType,
            rasterExtent: RasterExtent,
            epsg: Int,
            xskew: Double,
            yskew: Double,
            shouldCache: Boolean): RasterLayerInfo = {
    val tl = TileLayout(1, 1, rasterExtent.cols, rasterExtent.rows)
    RasterLayerInfo(id, cellType, rasterExtent, epsg, xskew, yskew, tl, shouldCache)
  }

  def apply(id: LayerId,
            cellType: CellType,
            rasterExtent: RasterExtent,
            epsg: Int,
            xskew: Double,
            yskew: Double,
            tileLayout: TileLayout): RasterLayerInfo = {
    RasterLayerInfo(id, cellType, rasterExtent, epsg, xskew, yskew, tileLayout, false)
  }

  /** Creates a RasterLayerInfo for an in memory raster based on the RasterDefinition
    */
  def fromDefinition(rd: RasterDefinition) =
    RasterLayerInfo(rd.layerId, rd.cellType, rd.rasterExtent, 0, 0.0, 0.0, rd.tileLayout, false)
}
