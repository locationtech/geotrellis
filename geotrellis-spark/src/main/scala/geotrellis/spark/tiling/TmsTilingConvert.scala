package geotrellis.spark.tiling
import geotrellis.RasterExtent
import geotrellis.process.LayerId

import geotrellis.raster.TileLayout
import geotrellis.source.RasterDefinition
import geotrellis.spark.metadata.PyramidMetadata

object TmsTilingConvert {

  /*
   * Conversion of RasterMetadata to RasterDefinition. In doing the conversion, we use the 
   * extent of the tile boundaries and not of the original image boundaries. Hence, we use 
   * tileToExtent instead of plain old 'extent' from PyramidMeta, which corresponds to the 
   * original image boundaries. Also ignored for the same reason is pixelExtent from the 
   * RasterMetadata
   */
  def rasterDefinition(zoom: Int, meta: PyramidMetadata): RasterDefinition = {
    val te = meta.rasterMetadata(zoom.toString).tileExtent
    val res = TmsTiling.resolution(zoom, meta.tileSize)
    val re = RasterExtent(TmsTiling.tileToExtent(te, zoom, meta.tileSize), res, res)

    // TODO - need to talk to Rob about converting TileLayout to longs
    val tl = TileLayout(re, te.width.toInt, te.height.toInt)
    RasterDefinition(LayerId.MEM_RASTER, re, tl, meta.rasterType, false)
  }

  /*
   * Conversion methods from/to Geotrellis Tile Identification scheme (referred to as 
   * gtTileId below) which differs from geotrellis-spark's tileId scheme (referred to as 
   * tileId) in two important ways:
   * 
   * 1. gtTileIds start from upper left and go down to lower right whereas tileIds start 
   * from lower left and go to upper right
   * 2. gtTileIds go from 0 to tiles-1 
   * 3. gtTileIds and their corresponding gtTx,gtTy are Ints whereas tileIds, tx, ty are Long
   * 
   * TODO: The last point needs to be addressed  
   */
  def fromGtTileId(gtTileId: Int, layout: TileLayout, te: TileExtent, zoom: Int): Long = {
    val (gtTx, gtTy) = layout.getXY(gtTileId)
    val tx = te.xmin + gtTx
    val ty = te.ymax - gtTy
    TmsTiling.tileId(tx, ty, zoom)
  }
  def fromGtTileIdX(gtTx: Int, te: TileExtent): Long = te.xmin + gtTx

  def fromGtTileIdY(gtTy: Int, te: TileExtent): Long = te.ymax - gtTy

  def toGtTileId(tileId: Long, layout: TileLayout, te: TileExtent, zoom: Int): Int = {
    val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
    val gtTx = (tx - te.xmin).toInt
    val gtTy = (te.ymax - ty).toInt
    layout.getTileIndex(gtTx, gtTy)
  }
  def toGtTileIdX(tx: Long, te: TileExtent): Int = (tx - te.xmin).toInt
  def toGtTileIdY(ty: Long, te: TileExtent): Int = (te.ymax - ty).toInt
}