/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark

import geotrellis._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.metadata._
import geotrellis.spark.cmd.NoDataHandler
import org.apache.hadoop.io.Writable

package object formats {
  type WritableTile = (TileIdWritable, ArgWritable)

  implicit class WritableTileWrapper(wt: WritableTile) {
    def toTile(meta: PyramidMetadata, zoom: Int, addUserNoData: Boolean = false): Tile = {
      val tileId = wt._1.get

      val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
      val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
      val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)

      val rd = wt._2.toRasterData(rasterType, tileSize, tileSize)
      if (addUserNoData) NoDataHandler.addUserNoData(rd, meta.nodata) 

      val raster = Raster(rd, RasterExtent(extent, tileSize, tileSize))

      Tile(tileId, raster)
    }
  }

  type PayloadWritableTile = (TileIdWritable, PayloadArgWritable)
  implicit class PayloadWritableTileWrapper(pwt: PayloadWritableTile) {
    def toPayloadTile(meta: PyramidMetadata, zoom: Int, payload: Writable, addUserNoData: Boolean = false): Tile = {
      val tileId = pwt._1.get

      val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
      val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
      val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)

      val rd = pwt._2.toPayloadRasterData(rasterType, tileSize, tileSize, payload)
      if(addUserNoData) NoDataHandler.addUserNoData(rd, meta.nodata) 

      val raster = Raster(rd, RasterExtent(extent, tileSize, tileSize))

      Tile(tileId, raster)
    }
  }
}
