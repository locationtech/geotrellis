package geotrellis.spark.op.zonal.summary

import geotrellis.raster.stats.Histogram
import geotrellis.raster.op.zonal.summary._
import geotrellis.raster.stats.FastMapHistogram

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.spark.rdd.RasterRDD

import geotrellis.vector._

import org.apache.spark.rdd.RDD
import org.apache.spark.Accumulator

trait ZonalSummaryRasterRDDMethods extends RasterRDDMethods {

  private def zonalSummary[B](
    polygon: Polygon,
    handleTileIntersection: TileIntersection => B,
    accumulator: Accumulator[B]): B = {
    val extentsRDD = rasterRDD.toExtentsRDD

    val sc = extentsRDD.sparkContext
    val polygonBroadcast = sc.broadcast(polygon)

    extentsRDD.foreach { case (extent, tmsTile) =>
      val tile = tmsTile.tile
      val p = polygonBroadcast.value
      if (p.contains(extent)) {
        accumulator += handleTileIntersection(FullTileIntersection(tile))
      } else {
        val polys = p.intersection(extent) match {
          case PolygonResult(intersectionPoly) => Seq(intersectionPoly)
          case MultiPolygonResult(mp) => mp.polygons.toSeq
          case _ => Seq()
        }

        val ptis = polys.map(PartialTileIntersection(tile, extent, _))

        for (pti <- ptis) accumulator += handleTileIntersection(pti)
      }
    }

    accumulator.value
  }

  def zonalHistogram(polygon: Polygon): RDD[Histogram] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(
      FastMapHistogram().asInstanceOf[Histogram])(
      HistogramAccumulatorParam
    )

    val histogram = zonalSummary(polygon, Histogram, accum)

    sc.parallelize(Array(histogram)) // Feels weird
  }

  def zonalMax(polygon: Polygon): RDD[Int] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(Int.MinValue)(MaxAccumulatorParam)

    val max = zonalSummary(polygon, Max, accum)

    sc.parallelize(Array(max)) // Feels weird
  }

  def zonalMaxDouble(polygon: Polygon): RDD[Double] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(Double.MinValue)(DoubleMaxAccumulatorParam)

    val max = zonalSummary(polygon, MaxDouble, accum)

    sc.parallelize(Array(max)) // Feels weird
  }

  def zonalMin(polygon: Polygon): RDD[Int] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(Int.MaxValue)(MinAccumulatorParam)

    val min = zonalSummary(polygon, Min, accum)

    sc.parallelize(Array(min)) // Feels weird
  }

  def zonalMinDouble(polygon: Polygon): RDD[Double] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(Double.MaxValue)(DoubleMinAccumulatorParam)

    val min = zonalSummary(polygon, MinDouble, accum)

    sc.parallelize(Array(min)) // Feels weird
  }

  def zonalMean(polygon: Polygon): RDD[Double] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(MeanResult(0, 0))(MeanResultAccumulatorParam)

    val mean = zonalSummary(polygon, Mean, accum).mean

    sc.parallelize(Array(mean)) // Feels weird
  }

  def zonalSum(polygon: Polygon): RDD[Long] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(0L)(SumAccumulatorParam)

    val sum = zonalSummary(polygon, Sum, accum)

    sc.parallelize(Array(sum)) // Feels weird
  }

  def zonalSumDouble(polygon: Polygon): RDD[Double] = {
    val sc = rasterRDD.sparkContext
    val accum = sc.accumulator(0.0)(DoubleSumAccumulatorParam)

    val sum = zonalSummary(polygon, SumDouble, accum)

    sc.parallelize(Array(sum)) // Feels weird
  }

}
