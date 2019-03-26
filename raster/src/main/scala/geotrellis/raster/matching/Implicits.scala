package geotrellis.raster.matching

import geotrellis.raster.{Tile, MultibandTile}


object Implicits extends Implicits

trait Implicits {
  implicit class withTileMatchingMethods(val self: Tile) extends SinglebandMatchingMethods

  implicit class withMultibandTileMatchingMethods(val self: MultibandTile) extends MultibandMatchingMethods
}
