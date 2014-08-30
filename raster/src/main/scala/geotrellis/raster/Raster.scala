package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

object Raster {
  implicit def tupToRaster(tup: (Tile, Extent)): Raster =
    Raster(tup._1, tup._2)

  implicit def tupSwapToRaster(tup: (Extent, Tile)): Raster =
    Raster(tup._2, tup._1)

  implicit def rasterToTile(r: Raster): Tile =
    r.tile
}

case class Raster(tile: Tile, extent: Extent) {
  lazy val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  def cols: Int = tile.cols
  def rows: Int = tile.rows

  def resample(target: RasterExtent): Raster =
    Raster(tile.resample(extent, target), target.extent)

  def resample(target: Extent): Raster =
    Raster(tile.resample(extent, target), target)

  def resample(targetCols: Int, targetRows: Int): Raster =
    Raster(tile.resample(extent, targetCols, targetRows), extent)

  def crop(target: Extent): Raster =
    Raster(tile.crop(extent, target), target)

  def reproject(src: CRS, dest: CRS): Raster =
    tile.reproject(extent, src, dest)
}
