/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.spark.tiling

import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.metadata.PyramidMetadata
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.Raster
import geotrellis.RasterExtent

case class TileIdRaster(tileId: Long, raster: Raster) 

object TileIdRaster {
  def from(writables: (TileIdWritable, ArgWritable), meta: PyramidMetadata, zoom: Int): TileIdRaster = {
    val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
    val (tw, aw) = writables
    val tileId = tw.get
    val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
    val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)
    val rd = aw.toRasterData(rasterType, tileSize, tileSize)
    val trd = NoDataHandler.removeGeotrellisNoData(rd, meta.nodata)
    val raster = Raster(trd, RasterExtent(extent, tileSize, tileSize))
    TileIdRaster(tileId, raster)
  }
}

case class TileIdCoordRaster(tileId: Long, tx: Long, ty: Long, raster: Raster) 

object TileIdCoordRaster {
  def from(writables: (TileIdWritable, ArgWritable), meta: PyramidMetadata, zoom: Int): TileIdCoordRaster = {
	val tr = TileIdRaster.from(writables, meta, zoom)	
    val (tx, ty) = TmsTiling.tileXY(tr.tileId, zoom)
    TileIdCoordRaster(tr.tileId, tx, ty, tr.raster)
  }
}