package geotrellis.raster.hydrology

import geotrellis.raster.Tile


object Implicits extends Implicits

trait Implicits {
  implicit class withTileHydrologyMethods(val self: Tile) extends HydrologyMethods
}
