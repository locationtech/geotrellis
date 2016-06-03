package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._


/**
  * A trait containing crop methods for [[MultibandTile]]s.
  */
trait MultibandTileCropMethods extends TileCropMethods[MultibandTile] {
  import Crop.Options

  /**
    * Given a [[GridBounds]] and some cropping options, crop the
    * [[MultibandTile]] and return a new MultibandTile.
    */
  def crop(gb: GridBounds, options: Options): MultibandTile = {
    val croppedBands = Array.ofDim[Tile](self.bandCount)

    for(b <- 0 until self.bandCount) {
      croppedBands(b) = self.band(b).crop(gb, options)
    }

    ArrayMultibandTile(croppedBands)
  }

  /**
    * Given a source Extent (the extent of the present
    * [[MultibandTile]]), a destination Extent, and a set of Options,
    * return a new MultibandTile.
    */
  def crop(srcExtent: Extent, extent: Extent, options: Options): MultibandTile =
    Raster(self, srcExtent).crop(extent, options).tile
}
