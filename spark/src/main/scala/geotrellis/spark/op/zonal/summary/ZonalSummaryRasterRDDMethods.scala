package geotrellis.spark.op.zonal.summary

import geotrellis.raster.stats.Histogram
import geotrellis.raster.stats.FastMapHistogram._
import geotrellis.raster.op.zonal.summary._

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.spark.rdd.RasterRDD

import geotrellis.vector._

import org.apache.spark.rdd.RDD

trait ZonalSummaryRasterRDDMethods extends RasterRDDMethods {

  def zonalHistogram(polygon: Polygon): RDD[Histogram] = {
    val extentsRDD = rasterRDD.toExtentsRDD

    val sc = extentsRDD.sparkContext
    val polygonBroadcast = sc.broadcast(polygon)
    val accum = sc.accumulator(Seq[Histogram]())(HistogramSeqAccumulatorParam)

    extentsRDD.foreach { case (extent, tmsTile) =>
      val tile = tmsTile.tile
      val p = polygonBroadcast.value
      if (p.contains(extent)) {
        accum += Seq(Histogram(FullTileIntersection(tile)))
      } else {
        val polys = p.intersection(extent) match {
          case PolygonResult(intersectionPoly) => Seq(intersectionPoly)
          case MultiPolygonResult(mp) => mp.polygons.toSeq
          case _ => Seq()
        }

        val ptis = polys.map(PartialTileIntersection(tile, extent, _))

        for (pti <- ptis) accum += Seq(Histogram(pti))
      }
    }

    val fastMapHistogram = fromHistograms(accum.value)

    sc.parallelize(Array(fastMapHistogram)) // feels wierd -.-
  }

  def zonalMax(extent: Extent, polygon: Polygon): RDD[(Long, Int)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, Max))
    }

  def zonalMaxDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, MaxDouble))
    }

  def zonalMin(extent: Extent, polygon: Polygon): RDD[(Long, Int)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, Min))
    }

  def zonalMinDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, MinDouble))
    }

  def zonalMean(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, Mean))
    }

  def zonalSum(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, Sum))
    }

  def zonalSumDouble(extent: Extent, polygon: Polygon): RDD[(Long, Double)] =
    rasterRDD.map {
      case TmsTile(t, r) => (t, r.zonalSummary(extent, polygon, SumDouble))
    }

}
