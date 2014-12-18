package geotrellis.spark.op.stats

import geotrellis.raster._
import geotrellis.raster.op.local._
import geotrellis.raster.stats.FastMapHistogram
import geotrellis.spark._
import org.apache.spark.SparkContext._
import geotrellis.raster.stats._


trait StatsRasterRDDMethods[K] extends RasterRDDMethods[K] {

  def averageByKey: RasterRDD[K] = {
    val createCombiner = (tile: Tile) => tile -> 1
    val mergeValue = (tup: (Tile, Int), tile2: Tile) => {
      val (tile1, count) = tup
      tile1 + tile2 -> (count + 1)
    }
    val mergeCombiners = (tup1: (Tile, Int), tup2: (Tile, Int)) => {
      val (tile1, count1) = tup1
      val (tile2, count2) = tup2
      tile1 + tile2 -> (count1 + count2)
    }
    asRasterRDD(rasterRDD.metaData) {
      rasterRDD
        .combineByKey(createCombiner, mergeValue, mergeCombiners)
        .mapValues { case (tile, count) => tile / count}
    }
  }

  def histogram: Histogram = {
    rasterRDD
      .map{ case (key, tile) => tile.histogram }
      .reduce { (h1, h2) => FastMapHistogram.fromHistograms(Array(h1,h2)) }
  }
}
