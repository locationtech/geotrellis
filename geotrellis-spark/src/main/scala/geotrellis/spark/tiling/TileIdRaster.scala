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
import geotrellis.Raster
import geotrellis.RasterExtent

import geotrellis.spark._
import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata

object TileIdRaster {
  def apply(writables: TileIdArgWritable, meta: PyramidMetadata, zoom: Int): TileIdRaster = {
	val (tileId, tx, ty, raster) = toTileIdCoordRaster(writables, meta, zoom)
    (tileId, raster)
  }

  def toTileIdCoordRaster(
      writables: TileIdArgWritable, 
      meta: PyramidMetadata, 
      zoom: Int, 
      addUserNoData: Boolean = false): TileIdCoordRaster = {
    val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
    val (tw, aw) = writables
    val tileId = tw.get
    val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
    val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)
    val rd = aw.toRasterData(rasterType, tileSize, tileSize)
    val trd = if(addUserNoData) NoDataHandler.addUserNoData(rd, meta.nodata) else rd
    val raster = Raster(trd, RasterExtent(extent, tileSize, tileSize))
    (tileId, tx, ty, raster)
  }

  def toTileIdArgWritable(tr: TileIdRaster): TileIdArgWritable = {
    (TileIdWritable(tr._1), ArgWritable.fromRasterData(tr._2.toArrayRaster.data))
  }
}