package geotrellis.spark.op.zonal

import geotrellis.raster.op.zonal._
import geotrellis.raster.op.stats._
import geotrellis.raster.histogram._
import geotrellis.raster._

import geotrellis.spark._

import org.apache.spark.rdd._
import org.apache.spark.SparkContext._
import spire.syntax.cfor._
import collection.immutable.HashMap

trait ZonalRasterRDDMethods[K] extends RasterRDDMethods[K] {

  private def mergeMaps(a: Map[Int, Histogram], b: Map[Int, Histogram]) = {
    var res = a
    for ((k, v) <- b)
      res = res + (k ->
        (
          if (res.contains(k)) FastMapHistogram.fromHistograms(Seq(res(k), v))
          else v
        )
      )

    res
  }

  def zonalHistogram(zonesRasterRDD: RasterRDD[K, Tile]): Map[Int, Histogram] =
    rasterRDD.join(zonesRasterRDD)
      .map((t: (K, (Tile, Tile))) => ZonalHistogram(t._2._1, t._2._2))
      .fold(Map[Int, Histogram]())(mergeMaps)

  def zonalPercentage(zonesRasterRDD: RasterRDD[K, Tile]): RasterRDD[K, Tile] = {
    val sc = rasterRDD.sparkContext
    val zoneHistogramMap = zonalHistogram(zonesRasterRDD)
    val zoneSumMap = zoneHistogramMap.map { case (k, v) => k -> v.getTotalCount }
    val bcZoneHistogramMap = sc.broadcast(zoneHistogramMap)
    val bcZoneSumMap = sc.broadcast(zoneSumMap)

    rasterRDD.combineTiles(zonesRasterRDD) { case (tile, zone) =>
      val zhm = bcZoneHistogramMap.value
      val zsm = bcZoneSumMap.value

      val (cols, rows) = (tile.cols, tile.rows)

      val res = IntArrayTile.empty(cols, rows)

      cfor(0)(_ < rows, _ + 1) { row =>
        cfor(0)(_ < cols, _ + 1) { col =>
          val (v, z) = (tile.get(col, row), zone.get(col, row))

          val (count, zoneCount) = (zhm(z).getItemCount(v), zsm(z))

          res.set(col, row, math.round((count / zoneCount.toDouble) * 100).toInt)
        }
      }

      res
    }
  }

}
