package geotrellis.raster.op.zonal

import geotrellis.raster._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._

trait ZonalMethods extends TileMethods {
  def zonalHistogramInt(zones: Tile): Map[Int, Histogram[Int]] =
    ZonalHistogramInt(tile, zones)

  def zonalStatisticsInt(zones: Tile): Map[Int, Statistics[Int]] =
    ZonalHistogramInt(tile, zones)
      .map { case (zone: Int, hist: Histogram[Int]) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalHistogramDouble(zones: Tile): Map[Int, Histogram[Double]] =
    ZonalHistogramDouble(tile, zones)

  def zonalStatisticsDouble(zones: Tile): Map[Int, Statistics[Double]] =
    ZonalHistogramDouble(tile, zones)
      .map { case (zone: Int, hist: Histogram[Double]) => (zone -> hist.generateStatistics) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(self, zones)
}
