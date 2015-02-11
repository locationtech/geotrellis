package geotrellis.raster.op

import geotrellis.raster.Tile
import geotrellis.raster.op.stats.StatsMethods

package object stats {
  implicit class StatsMethodExtensions(val tile: Tile) extends StatsMethods { }
}
