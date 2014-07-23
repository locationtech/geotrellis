package geotrellis.raster.op.zonal

import geotrellis.raster._

package object summary {
  implicit class ZonalSummaryMethodExtensions(val tile: Tile) extends ZonalSummaryMethods { }
}
