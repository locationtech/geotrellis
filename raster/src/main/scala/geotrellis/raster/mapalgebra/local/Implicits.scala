package geotrellis.raster.mapalgebra.local

import geotrellis.raster.Tile


object Implicits extends Implicits

trait Implicits {
  implicit class withTileLocalMethods(val self: Tile) extends LocalMethods

  implicit class withTileSeqLocalMethods(val self: Traversable[Tile]) extends LocalSeqMethods
}
