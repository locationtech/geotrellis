package geotrellis.raster.mapalgebra.zonal

import geotrellis.raster._
import geotrellis.raster.summary._
import geotrellis.raster.histogram._
import geotrellis.util.MethodExtensions


trait ZonalMethods extends MethodExtensions[Tile] {
  def zonalHistogramInt(zones: Tile): Map[Int, Histogram[Int]] =
    IntZonalHistogram(self, zones)

  def zonalStatisticsInt(zones: Tile): Map[Int, Statistics[Int]] =
    IntZonalHistogram(self, zones)
      .map { case (zone: Int, hist: Histogram[Int]) => (zone -> hist.statistics.get) }
      .toMap

  def zonalHistogramDouble(zones: Tile): Map[Int, Histogram[Double]] =
    DoubleZonalHistogram(self, zones)

  def zonalStatisticsDouble(zones: Tile): Map[Int, Statistics[Double]] =
    DoubleZonalHistogram(self, zones)
      .map { case (zone: Int, hist: Histogram[Double]) => (zone -> hist.statistics.get) }
      .toMap

  def zonalPercentage(zones: Tile): Tile =
    ZonalPercentage(self, zones)
}
