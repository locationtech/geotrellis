package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector._

trait SinglebandTileResampleMethods extends TileResampleMethods[Tile] {
  def resample(extent: Extent, target: RasterExtent, method: ResampleMethod): Tile =
    Raster(self, extent).resample(target, method).tile

  def resample(extent: Extent, targetCols: Int, targetRows: Int, method: ResampleMethod): Tile =
    Raster(self, extent).resample(targetCols, targetRows, method).tile
}
