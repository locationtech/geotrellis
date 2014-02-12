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

  def toTileIdCoordRaster(writables: TileIdArgWritable, meta: PyramidMetadata, zoom: Int): TileIdCoordRaster = {
    val (tileSize, rasterType) = (meta.tileSize, meta.rasterType)
    val (tw, aw) = writables
    val tileId = tw.get
    val (tx, ty) = TmsTiling.tileXY(tileId, zoom)
    val extent = TmsTiling.tileToExtent(tx, ty, zoom, tileSize)
    val rd = aw.toRasterData(rasterType, tileSize, tileSize)
    val trd = NoDataHandler.removeGeotrellisNoData(rd, meta.nodata)
    val raster = Raster(trd, RasterExtent(extent, tileSize, tileSize))
    (tileId, tx, ty, raster)
  }

  def toTileIdArgWritable(tr: TileIdRaster): TileIdArgWritable = {
    (TileIdWritable(tr._1), ArgWritable.fromRasterData(tr._2.toArrayRaster.data))
  }
}