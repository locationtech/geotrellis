package geotrellis.raster.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._

trait ZonalMethods extends TileMethods {
  def zonalHistogram(zones: Tile): Map[Int, Histogram] =
    ZonalHistogram(tile, zones)

  def zonalStatistics(zones: Tile): Map[Int, Statistics] =
    ZonalHistogram(tile, zones)
      .map { case (zone: Int, hist: Histogram) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(tile, zones)
}
