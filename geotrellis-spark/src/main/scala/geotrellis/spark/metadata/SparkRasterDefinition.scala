package geotrellis.spark.metadata

import geotrellis.source.RasterDefinition
import geotrellis.spark.tiling.TileExtent
import geotrellis.raster.TileLayout
import geotrellis.spark.tiling.TmsTiling
import geotrellis.process.LayerId
import geotrellis.RasterExtent
import geotrellis.RasterType

/* 
 * A RasterDefinition plus a few more fields to facilitate reconstruction of RasterMetadata
 */
class SparkRasterDefinition(
  val zoom: Int,
  val tileExtent: TileExtent,
  rasterExtent: RasterExtent,
  tileLayout: TileLayout,
  rasterType: RasterType)
  extends RasterDefinition(LayerId.MEM_RASTER, rasterExtent, tileLayout, rasterType, false)

object SparkRasterDefinition {

  /*
   * Conversion of RasterMetadata to RasterDefinition. In doing the conversion, we use the 
   * extent of the tile boundaries and not of the original image boundaries. Hence, we use 
   * tileToExtent instead of plain old 'extent' from PyramidMeta, which corresponds to the 
   * original image boundaries. Also ignored for the same reason is pixelExtent from the 
   * RasterMetadata
   */
  def apply(zoom: Int, meta: PyramidMetadata) = {
    val te = meta.rasterMetadata(zoom.toString).tileExtent
    val res = TmsTiling.resolution(zoom, meta.tileSize)
    val re = RasterExtent(TmsTiling.tileToExtent(te, zoom, meta.tileSize), res, res)

    // TODO - need to talk to Rob about converting TileLayout to longs
    val tl = TileLayout(re, te.width.toInt, te.height.toInt)
    new SparkRasterDefinition(zoom, te, re, tl, meta.rasterType)
  }

  def apply(zoom: Int, te: TileExtent, re: RasterExtent, tl: TileLayout, rt: RasterType) =
    new SparkRasterDefinition(zoom, te, re, tl, rt)
}