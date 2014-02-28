package geotrellis.spark

import geotrellis.Raster
import geotrellis.RasterExtent

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.cmd.NoDataHandler
import geotrellis.spark.formats.ArgWritable
import geotrellis.spark.formats.TileIdWritable
import geotrellis.spark.metadata.PyramidMetadata

case class Tile(id: Long, raster: Raster) {
  def tileXY(zoom: Int) =  TmsTiling.tileXY(id, zoom)
}

object Tile {
  implicit def tupleToTile(t:(Long,Raster)): Tile =
    Tile(t._1, t._2)

  def apply(writables: TileIdArgWritable, meta: PyramidMetadata, zoom: Int): Tile = {
    val (tileId, tx, ty, raster) = toTileIdCoordRaster(writables, meta, zoom)
    Tile(tileId, raster)
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

  def toTileIdArgWritable(tr: Tile): TileIdArgWritable = {
    (TileIdWritable(tr.id), ArgWritable.fromRasterData(tr.raster.data))
  }
}
