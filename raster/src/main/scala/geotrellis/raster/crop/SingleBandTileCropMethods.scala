package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait SingleBandTileCropMethods extends TileCropMethods[Tile] {
  import Crop.Options

  def crop(gb: GridBounds, options: Options): Tile = {
    val cropBounds =
      if(options.clamp)
        gb.clampTo(self)
      else
        gb

    val res = CroppedTile(self, cropBounds)
    if(options.force) res.toArrayTile else res
  }

  def crop(srcExtent: Extent, extent: Extent, options: Options): Tile =
    Raster(self, srcExtent).crop(extent, options).tile
}
