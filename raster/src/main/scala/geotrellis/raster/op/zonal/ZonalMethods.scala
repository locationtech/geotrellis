package geotrellis.raster.op.zonal

import geotrellis.raster._
import geotrellis.raster.stats.Histogram

trait ZonalMethods extends TileMethods {
  def zonalHistogram(zones: Tile): Map[Int, Histogram] =
    ZonalHistogram(tile, zones)

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(tile, zones)
}
