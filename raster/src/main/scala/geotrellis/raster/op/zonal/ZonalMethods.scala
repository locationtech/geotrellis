package geotrellis.raster.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._

trait ZonalMethods extends MethodExtensions[Tile] {
  def zonalHistogram(zones: Tile): Map[Int, Histogram] =
    ZonalHistogram(self, zones)

  def zonalStatistics(zones: Tile): Map[Int, Statistics] =
    ZonalHistogram(self, zones)
      .map { case (zone: Int, hist: Histogram) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(self, zones)
}
