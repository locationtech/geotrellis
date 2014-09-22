package geotrellis.spark.op.zonal.summary

import geotrellis.raster.stats.Histogram
import geotrellis.raster.op.zonal.summary._

import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

import geotrellis.vector._

import org.apache.spark.rdd.RDD

trait ZonalSummaryRasterRDDMethods extends RasterRDDMethods {

  def zonalHistogram(
    extent: Extent,
    polygon: Polygon): RDD[(Long, Histogram)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, Histogram))
    }

  def zonalMax(extent: Extent, polygon: Polygon): RDD[(Long, Int)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, Max))
    }

  def zonalMaxDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, MaxDouble))
    }

  def zonalMin(extent: Extent, polygon: Polygon): RDD[(Long, Int)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, Min))
    }

  def zonalMinDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, MinDouble))
    }

  def zonalMean(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, Mean))
    }

  def zonalSum(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, Sum))
    }

  def zonalSumDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(r, t) => (r, t.zonalSummary(extent, polygon, SumDouble))
    }

}
