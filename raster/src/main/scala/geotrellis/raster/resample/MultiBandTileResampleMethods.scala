package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector._

trait MultiBandTileResampleMethods extends TileResampleMethods[MultiBandTile] {
  def resample(extent: Extent, target: RasterExtent, method: ResampleMethod): MultiBandTile =
    Raster(self, extent).resample(target, method).tile

  def resample(extent: Extent, target: Extent, method: ResampleMethod): MultiBandTile =
    Raster(self, extent).resample(target, method).tile

  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): MultiBandTile =
    Raster(self, extent).resample(targetCols, targetRows, method).tile
}
