package geotrellis.raster.op

import geotrellis.raster._

package object zonal {
  implicit class ZonalMethodExtensions(val tile: Tile) extends ZonalMethods { }
}
