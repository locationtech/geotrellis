package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.vector._
import geotrellis.proj4.CRS

object Raster {
  def apply(feature: Feature[Extent, Tile]): Raster =
    Raster(feature.data, feature.geom)

  implicit def tupToRaster(tup: (Tile, Extent)): Raster =
    Raster(tup._1, tup._2)

  implicit def tupSwapToRaster(tup: (Extent, Tile)): Raster =
    Raster(tup._2, tup._1)

  implicit def rasterToTile(r: Raster): Tile =
    r.tile

  implicit def rasterToFeature(r: Raster): Feature[Extent, Tile] =
    r.asFeature

  implicit def featureToRaster(feature: Feature[Extent, Tile]): Raster =
     apply(feature)
}

case class Raster(tile: Tile, extent: Extent) extends Product2[Tile, Extent] {
  lazy val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  def cols: Int = tile.cols
  def rows: Int = tile.rows

  def asFeature(): Feature[Extent, Tile] = ExtentFeature(extent, tile)

  def getValueAtPoint(point: Point): Int =
    getValueAtPoint(point.x, point.y)

  def getValueAtPoint(x: Double, y: Double): Int =
    tile.get(
      rasterExtent.mapXToGrid(x),
      rasterExtent.mapYToGrid(y)
    )

  def getDoubleValueAtPoint(point: Point): Double =
    getDoubleValueAtPoint(point.x, point.y)

  def getDoubleValueAtPoint(x: Double, y: Double): Double =
    tile.getDouble(
      rasterExtent.mapXToGrid(x),
      rasterExtent.mapYToGrid(y)
    )

  def resample(target: RasterExtent): Raster =
    Raster(tile.resample(extent, target), target.extent)

  def resample(target: Extent): Raster =
    Raster(tile.resample(extent, target), target)

  def resample(targetCols: Int, targetRows: Int): Raster =
    Raster(tile.resample(extent, targetCols, targetRows), extent)

  def crop(target: Extent): Raster =
    Raster(tile.crop(extent, target), target)

  def reproject(src: CRS, dest: CRS): SingleBandReproject.Apply =
    tile.reproject(extent, src, dest)

  def _1 = tile

  def _2 = extent
}
