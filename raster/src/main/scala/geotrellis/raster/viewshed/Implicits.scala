package geotrellis.raster.viewshed

import geotrellis.raster.Tile


object Implicits extends Implicits

trait Implicits {
  implicit class withTileViewshedMethods(val self: Tile) extends ViewshedMethods
}
