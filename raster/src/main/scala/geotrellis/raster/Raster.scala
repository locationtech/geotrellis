package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.raster.resample._
import geotrellis.vector._
import geotrellis.proj4.CRS

object Raster {
  def apply[T <: CellGrid](feature: Feature[Extent, T]): Raster[T] =
    Raster(feature.data, feature.geom)

  implicit def tupToRaster(tup: (Tile, Extent)): Raster[Tile] =
    Raster(tup._1, tup._2)

  implicit def tupSwapToRaster(tup: (Extent, Tile)): Raster[Tile] =
    Raster(tup._2, tup._1)

  implicit def rasterToTile[T <: CellGrid](r: Raster[T]): T =
    r.tile

  implicit def rasterToFeature[T <: CellGrid](r: Raster[T]): Feature[Extent, T] =
    r.asFeature

  implicit def featureToRaster[T <: CellGrid](feature: Feature[Extent, T]): Raster[T] =
     apply(feature)
}

case class Raster[+T <: CellGrid](tile: T, extent: Extent) extends Product2[T, Extent] {
  lazy val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  def cols: Int = tile.cols
  def rows: Int = tile.rows

  def asFeature(): Feature[Extent, T] = ExtentFeature(extent, tile: T)

  def _1 = tile

  def _2 = extent
}
