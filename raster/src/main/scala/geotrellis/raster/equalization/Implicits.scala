package geotrellis.raster.equalization

import geotrellis.raster.{Tile, MultibandTile}


object Implicits extends Implicits

trait Implicits {
  implicit class withTileEqualizationMethods(val self: Tile) extends SinglebandEqualizationMethods

  implicit class withMultibandTileEqualizationMethods(val self: MultibandTile) extends MultibandEqualizationMethods
}
