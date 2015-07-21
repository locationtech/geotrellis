package geotrellis.raster

import geotrellis.raster.reproject._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

object MultiBandRaster {
  implicit def tupToMultiBandRaster(tup: (MultiBandTile, Extent)): MultiBandRaster =
    MultiBandRaster(tup._1, tup._2)

  implicit def tupSwapToMultiBandRaster(tup: (Extent, MultiBandTile)): MultiBandRaster =
    MultiBandRaster(tup._2, tup._1)

  implicit def rasterToTile(r: MultiBandRaster): MultiBandTile =
    r.tile
}

case class MultiBandRaster(tile: MultiBandTile, extent: Extent) {
  lazy val rasterExtent = RasterExtent(extent, tile.cols, tile.rows)

  def cols: Int = tile.cols
  def rows: Int = tile.rows

  // def resample(target: RasterExtent): MultiBandRaster =
  //   MultiBandRaster(tile.resample(extent, target), target.extent)

  // def resample(target: Extent): MultiBandRaster =
  //   MultiBandRaster(tile.resample(extent, target), target)

  // def resample(targetCols: Int, targetRows: Int): MultiBandRaster =
  //   MultiBandRaster(tile.resample(extent, targetCols, targetRows), extent)

  // def crop(target: Extent): MultiBandRaster =
  //   MultiBandRaster(tile.crop(extent, target), target)

  // def reproject(src: CRS, dest: CRS): MultiBandRaster =
  //   tile.reproject(extent, src, dest)

  // def reproject(method: InterpolationMethod, src: CRS, dest: CRS): MultiBandRaster =
  //   tile.reproject(extent, src, dest, ReprojectOptions(method = method))

  // def reproject(src: CRS, dest: CRS, options: ReprojectOptions): MultiBandRaster =
  //   tile.reproject(extent, src, dest, options)

}
