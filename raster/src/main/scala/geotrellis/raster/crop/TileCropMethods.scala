package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait TileCropMethods extends TileMethods with CropMethods[Tile] {
  def crop(gb: GridBounds, force: Boolean): Tile = {
    val res = CroppedTile(tile, gb)
    if(force) res.toArrayTile else res
  }
}
