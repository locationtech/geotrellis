/**************************************************************************
 * Copyright (c) 2014 Digital Globe.
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
 **************************************************************************/

package geotrellis.spark

import geotrellis._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.metadata._
import geotrellis.spark.cmd.NoDataHandler

package object formats {
  type WritableTile = (TileIdWritable, ArgWritable)

  implicit class WritableTileWrapper(wt: WritableTile) {
    def toTile(meta: PyramidMetadata, zoom: Int, addUserNoData: Boolean = false): Tile = {
      val tileId = wt._1.get

      val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
      val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
      val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)

      val rd = wt._2.toRasterData(rasterType, tileSize, tileSize)
      val trd = if(addUserNoData) NoDataHandler.addUserNoData(rd, meta.nodata) else rd

      val raster = Raster(trd, RasterExtent(extent, tileSize, tileSize))

      Tile(tileId, raster)
    }
  }
}
