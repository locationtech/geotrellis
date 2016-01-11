package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

trait MultiBandRasterResampleMethods extends RasterResampleMethods[MultiBandRaster] {
  def resample(target: RasterExtent, method: ResampleMethod): MultiBandRaster = {
    val tile = self.tile
    val extent = self.extent
    val bandCount = tile.bandCount
    val resampledBands = Array.ofDim[Tile](bandCount)

    cfor(0)(_ < bandCount, _ + 1) { b =>
      resampledBands(b) = Raster(tile.band(b), extent).resample(target, method).tile
    }

    Raster(ArrayMultiBandTile(resampledBands), target.extent)
  }
}
