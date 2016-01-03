package geotrellis.raster.crop

import geotrellis.vector._
import geotrellis.raster._

trait TileCropMethods extends CropMethods[Tile] {
  def crop(gb: GridBounds, force: Boolean): Tile = {
    val res = CroppedTile(self, gb)
    if(force) res.toArrayTile else res
  }
}
