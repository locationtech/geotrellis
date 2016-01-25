package geotrellis.raster.resample

import geotrellis.raster._
import geotrellis.vector.Extent

import spire.syntax.cfor._

trait MultibandRasterResampleMethods extends RasterResampleMethods[MultibandRaster] {
  def resample(target: RasterExtent, method: ResampleMethod): MultibandRaster = {
    val tile = self.tile
    val extent = self.extent
    val bandCount = tile.bandCount
    val resampledBands = Array.ofDim[Tile](bandCount)

    cfor(0)(_ < bandCount, _ + 1) { b =>
      resampledBands(b) = Raster(tile.band(b), extent).resample(target, method).tile
    }

    Raster(ArrayMultibandTile(resampledBands), target.extent)
  }
}
