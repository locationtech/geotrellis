package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait MultiBandTileCropMethods extends TileCropMethods[MultiBandTile] {
  import Crop.Options

  def crop(gb: GridBounds, options: Options): MultiBandTile = {
    val croppedBands = Array.ofDim[Tile](self.bandCount)

    for(b <- 0 until self.bandCount) {
      croppedBands(b) = self.band(b).crop(gb, options)
    }

    ArrayMultiBandTile(croppedBands)
  }

  def crop(srcExtent: Extent, extent: Extent, options: Options): MultiBandTile =
    Raster(self, srcExtent).crop(extent, options).tile
}
