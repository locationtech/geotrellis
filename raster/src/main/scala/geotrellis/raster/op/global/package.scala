package geotrellis.raster.op

import geotrellis.raster._

package object global {
  implicit class GlobalMethodExtensions(val tile: Tile) extends GlobalMethods { }
}
