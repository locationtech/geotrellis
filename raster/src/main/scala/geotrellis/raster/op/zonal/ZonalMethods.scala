package geotrellis.raster.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._

trait ZonalMethods extends MethodExtensions[Tile] {
  def zonalHistogramInt(zones: Tile): Map[Int, Histogram[Int]] =
    ZonalHistogramInt(self, zones)

  def zonalStatisticsInt(zones: Tile): Map[Int, Statistics[Int]] =
    ZonalHistogramInt(self, zones)
      .map { case (zone: Int, hist: Histogram[Int]) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalHistogramDouble(zones: Tile): Map[Int, Histogram[Double]] =
    ZonalHistogramDouble(self, zones)

  def zonalStatisticsDouble(zones: Tile): Map[Int, Statistics[Double]] =
    ZonalHistogramDouble(self, zones)
      .map { case (zone: Int, hist: Histogram[Double]) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(self, zones)
}
