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

package geotrellis.spark.metadata

import geotrellis.RasterExtent
import geotrellis.RasterType
import geotrellis.process.LayerId
import geotrellis.raster.TileLayout

import geotrellis.source.RasterDefinition
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

/* 
 * This is passed between operations and has the RasterDefinition. It also has fields necessary 
 * to reconstruct the original PyramidMetadata. 
 * 
 * Also includes conversions in both directions (RasterDefinition -> RasterMetadata and back)
 */
case class Context(zoom: Int, tileExtent: TileExtent, userNodata: Double, rasterDefinition: RasterDefinition) {

  /*
   * Conversion of RasterDefinition to RasterMetadata. Note that the created metadata has  
   * its extent field set to tile extents from RasterDefinition (see fromMetadata below) 
   * on the reasoning behind this. Similarly for the pixelExtent field of RasterMetadata
   */
  def toMetadata: PyramidMetadata = {
    assert(rasterDefinition.tileLayout.pixelCols == TmsTiling.DefaultTileSize)

    val pe = TmsTiling.extentToPixel(
      rasterDefinition.rasterExtent.extent,
      zoom,
      rasterDefinition.tileLayout.pixelCols)

    PyramidMetadata(
      rasterDefinition.rasterExtent.extent,
      rasterDefinition.tileLayout.pixelCols,
      PyramidMetadata.MaxBands,
      userNodata,
      RasterType.toAwtType(rasterDefinition.rasterType),
      zoom,
      Map(zoom.toString -> RasterMetadata(pe, tileExtent)))
  }
}

object Context {
  /*
   * Conversion of RasterMetadata to RasterDefinition. In doing the conversion, we use the 
   * extent of the tile boundaries and not of the original image boundaries. Hence, we use 
   * tileToExtent instead of plain old 'extent' from PyramidMeta, which corresponds to the 
   * original image boundaries. Also ignored for the same reason is pixelExtent from the 
   * RasterMetadata
   */
  def fromMetadata(zoom: Int, meta: PyramidMetadata) = {
    val te = meta.rasterMetadata(zoom.toString).tileExtent
    val res = TmsTiling.resolution(zoom, meta.tileSize)
    val re = RasterExtent(TmsTiling.tileToExtent(te, zoom, meta.tileSize), res, res)

    // TODO - once TileLayout supports longs, remove the to.Int
    val tl = TileLayout(re, te.width.toInt, te.height.toInt)
    Context(zoom, te, meta.nodata, re, tl, meta.rasterType)
  }

  def apply(
    zoom: Int,
    tileExtent: TileExtent,
    userNodata: Double,
    rasterExtent: RasterExtent,
    tileLayout: TileLayout,
    rasterType: RasterType) =
    new Context(
      zoom,
      tileExtent,
      userNodata,
      RasterDefinition(LayerId.MEM_RASTER, rasterExtent, tileLayout, rasterType, false))
}
