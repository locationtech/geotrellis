package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait MultiBandTileCropMethods extends CropMethods[MultiBandTile] {
  def crop(gb: GridBounds, force: Boolean): MultiBandTile = {
    val croppedBands = Array.ofDim[Tile](self.bandCount)

    for(b <- 0 until self.bandCount) {
      val cropped = self.band(b).crop(gb)
      if(force) {
        croppedBands(b) = cropped.toArrayTile
      } else {
        croppedBands(b) = cropped
      }
    }

    ArrayMultiBandTile(croppedBands)
  }
}
