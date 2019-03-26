package geotrellis.raster.mapalgebra.focal

import geotrellis.raster.Tile


object Implicits extends Implicits


trait Implicits {
  implicit class withTileFocalMethods(val self: Tile) extends FocalMethods
}
