package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait MultibandTileCropMethods extends TileCropMethods[MultibandTile] {
  import Crop.Options

  def crop(gb: GridBounds, options: Options): MultibandTile = {
    val croppedBands = Array.ofDim[Tile](self.bandCount)

    for(b <- 0 until self.bandCount) {
      croppedBands(b) = self.band(b).crop(gb, options)
    }

    ArrayMultibandTile(croppedBands)
  }

  def crop(srcExtent: Extent, extent: Extent, options: Options): MultibandTile =
    Raster(self, srcExtent).crop(extent, options).tile
}
