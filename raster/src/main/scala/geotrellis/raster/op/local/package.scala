package geotrellis.raster.op

import geotrellis.raster._

package object local {
  implicit class LocalExtensions(val tile: Tile) extends LocalMethods { }
}
