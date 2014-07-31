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

package geotrellis.spark.metadata

import geotrellis.raster._
import geotrellis.raster.TileLayout
import geotrellis.spark.rdd.TileIdPartitioner
import geotrellis.spark.tiling.TileExtent
import geotrellis.spark.tiling.TmsTiling

case class RasterDefinition(rasterExtent: RasterExtent,
                            tileLayout: TileLayout,
                            cellType: CellType) {
  def isTiled = tileLayout.isTiled

  def withType(newType: CellType) =
    new RasterDefinition(rasterExtent, tileLayout, newType)
}

/* 
 * The Context is passed between operations, and contains two key fields used to describe and persist 
 * a RasterRDD: RasterDefinition and TileIdPartitioner.
 * 
 * See 'apply' variants for how to build a Context and 'extract' for how to get the original PyramidMetadata
 * and TileIdPartitioner back to, say, save the RasterRDD.
 */

class Context(val zoom: Int,
              val tileExtent: TileExtent,
              val userNodata: Double,
              val rasterDefinition: RasterDefinition,
              val partitioner: TileIdPartitioner)
  extends Serializable {

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
      CellType.toAwtType(rasterDefinition.cellType),
      zoom,
      Map(zoom.toString -> RasterMetadata(pe, tileExtent)))
  }

}

object Context {
  /*
   * Construct a context given a PyramidMetadata and TileIdPartitioner
   * 
   * In converting the PyramidMetadata's RasterMetadata to RasterDefinition, we use the 
   * extent of the tile boundaries and not of the original image boundaries. Hence, we use 
   * tileToExtent instead of plain old 'extent' from PyramidMeta, which corresponds to the 
   * original image boundaries. Also ignored for the same reason is pixelExtent from the 
   * RasterMetadata
   */
  def apply(zoom: Int, meta: PyramidMetadata, partitioner: TileIdPartitioner): Context = {
    val te = meta.rasterMetadata(zoom.toString).tileExtent
    val res = TmsTiling.resolution(zoom, meta.tileSize)
    val re = RasterExtent(TmsTiling.tileToExtent(te, zoom, meta.tileSize), res, res)
    val tl = TileLayout(te.width.toInt, te.height.toInt, meta.tileSize, meta.tileSize)
    Context(zoom, te, meta.nodata, re, tl, meta.cellType, partitioner)
  }

  def apply(
    zoom: Int,
    tileExtent: TileExtent,
    userNodata: Double,
    rasterExtent: RasterExtent,
    tileLayout: TileLayout,
    cellType: CellType,
    partitioner: TileIdPartitioner): Context =
    new Context(
      zoom,
      tileExtent,
      userNodata,
      RasterDefinition(rasterExtent, tileLayout, cellType),
      partitioner)

  def unapply(c: Context): Option[(PyramidMetadata, TileIdPartitioner)] = Some(c.toMetadata, c.partitioner)

}
