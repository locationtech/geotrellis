package geotrellis.raster.mask

import geotrellis.raster._

object Implicits extends Implicits

trait Implicits {
  implicit class withTileMaskMethods(val self: Tile) extends TileMaskMethods
}
