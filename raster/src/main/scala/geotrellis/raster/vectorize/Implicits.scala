package geotrellis.raster.vectorize

import geotrellis.raster.{Tile, Raster}


object Implicits extends Implicits

trait Implicits {
  implicit class withSinglebandVectorizeMethods(val self: Tile) extends TileVectorizeMethods

  implicit class withSinglebandRasterVectorizeMethods(val self: Raster[Tile]) extends SinglebandRasterVectorizeMethods
}
