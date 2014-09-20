package geotrellis.spark.op.zonal.summary

import geotrellis.raster.stats.Histogram
import geotrellis.raster.op.zonal.summary._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import geotrellis.vector._

trait ZonalSummaryRasterRDDMethods extends RasterRDDMethods {

  def zonalHistogram(extent: Extent, polygon: Polygon): Seq[Histogram] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, Histogram))

  def zonalMax(extent: Extent, polygon: Polygon): Seq[Int] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, Max))

  def zonalMaxDouble(extent: Extent, polygon: Polygon): Seq[Double] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, MaxDouble))

  def zonalMin(extent: Extent, polygon: Polygon): Seq[Int] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, Min))

  def zonalMinDouble(extent: Extent, polygon: Polygon): Seq[Double] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, MinDouble))

  def zonalMean(extent: Extent, polygon: Polygon): Seq[Double] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, Mean))

  def zonalSum(extent: Extent, polygon: Polygon): Seq[Long] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, Sum))

  def zonalSumDouble(extent: Extent, polygon: Polygon): Seq[Double] =
    rasterRDD
      .collect
      .map(tmsTile => tmsTile.tile.zonalSummary(extent, polygon, SumDouble))

}
