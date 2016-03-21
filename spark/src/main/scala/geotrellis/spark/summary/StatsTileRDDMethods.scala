package geotrellis.spark.summary

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.summary._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._


trait StatsTileRDDMethods[K] extends TileRDDMethods[K] {

  def averageByKey(partitioner: Option[Partitioner] = None): RDD[(K, Tile)] = {
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
    partitioner
      .fold(self.combineByKey(createCombiner, mergeValue, mergeCombiners))(self.combineByKey(createCombiner, mergeValue, mergeCombiners, _))
      .mapValues { case (tile, count) => tile / count}
  }

  def histogram: Histogram[Int] = {
    self
      .map { case (key, tile) => tile.histogram }
      .reduce { _ merge _ }
  }

  def histogramDouble: Histogram[Double] = {
    self
      .map { case (key, tile) => tile.histogramDouble }
      .reduce { _ merge _ }
  }

  def classBreaks(numBreaks: Int): Array[Int] =
    histogram.quantileBreaks(numBreaks)

  def classBreaksDouble(numBreaks: Int): Array[Double] =
    histogramDouble.quantileBreaks(numBreaks)

  def minMax: (Int, Int) =
    self.map(_._2.findMinMax)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }

  def minMaxDouble: (Double, Double) =
    self
      .map(_._2.findMinMaxDouble)
      .reduce { (t1, t2) =>
        val (min1, max1) = t1
        val (min2, max2) = t2
        val min =
          if(isNoData(min1)) min2
          else {
            if(isNoData(min2)) min1
            else math.min(min1, min2)
          }
        val max =
          if(isNoData(max1)) max2
          else {
            if(isNoData(max2)) max1
            else math.max(max1, max2)
          }
        (min, max)
      }
}
