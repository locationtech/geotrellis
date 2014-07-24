package geotrellis.raster.op

import geotrellis.raster._

package object focal {
  implicit class FocalExtensions(val tile: Tile) extends FocalMethods { }
}
