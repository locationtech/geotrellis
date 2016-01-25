package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector._

trait MultibandTileResampleMethods extends TileResampleMethods[MultibandTile] {
  def resample(extent: Extent, target: RasterExtent, method: ResampleMethod): MultibandTile =
    Raster(self, extent).resample(target, method).tile

  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): MultibandTile =
    Raster(self, extent).resample(targetCols, targetRows, method).tile
}
