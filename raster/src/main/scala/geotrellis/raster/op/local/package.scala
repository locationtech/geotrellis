package geotrellis.raster.op

import geotrellis.raster._

package object local {
  implicit class LocalExtensions(val tile: Tile) extends LocalMethods { }
  implicit class LocalSeqExtensions(val tiles: Traversable[Tile]) extends LocalSeqMethods { }
}
