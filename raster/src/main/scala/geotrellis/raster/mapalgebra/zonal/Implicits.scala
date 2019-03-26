package geotrellis.raster.mapalgebra.zonal

import geotrellis.raster.Tile


object Implicits extends Implicits


trait Implicits {
  implicit class withTileZonalMethods(val self: Tile) extends ZonalMethods
}
