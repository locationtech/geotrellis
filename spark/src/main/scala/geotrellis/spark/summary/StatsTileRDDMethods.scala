/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.summary

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.raster.mapalgebra.local._
import geotrellis.raster.summary._
import geotrellis.spark._
import geotrellis.util.MethodExtensions

import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

import scala.reflect.ClassTag

abstract class StatsTileRDDMethods[K: ClassTag] extends MethodExtensions[RDD[(K, Tile)]] {

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

  /**
    * Compute the histogram of an RDD of [[Tile]] objects.
    *
    * @return  A [[Histogram[Double]]]
    */
  def histogram(): Histogram[Double] =
    histogram(StreamingHistogram.DEFAULT_NUM_BUCKETS)

  /**
    * Compute the histogram of an RDD of [[Tile]] objects.
    *
    * @param  numBuckets  The number of buckets that the histogram should have
    * @param  fraction    The fraction of [[Tile]] objects to sample (the default is 100%)
    * @param  seed        The seed of the RNG which determines which tiles to take the histograms of
    * @return             A [[Histogram[Double]]]
    */
  def histogram(numBuckets: Int, fraction: Double = 1.0, seed: Long = 33): Histogram[Double] =
    (if (fraction >= 1.0) self; else self.sample(false, fraction, seed))
      .map { case (key, tile) => tile.histogramDouble(numBuckets) }
      .reduce { _ merge _ }

  /** Gives a histogram that uses exact counts of integer values.
    *
    * @note This cannot handle counts that are larger than Int.MaxValue, and
    *       should not be used with very large datasets whose counts will overflow.
    *       These histograms can get very large with a wide range of values.
    */
  def histogramExactInt: Histogram[Int] = {
    self
      .map { case (key, tile) => tile.histogram }
      .reduce { _ merge _ }
  }

  def classBreaks(numBreaks: Int): Array[Int] =
    classBreaksDouble(numBreaks).map(_.toInt)

  def classBreaksDouble(numBreaks: Int): Array[Double] =
    histogram(numBreaks).quantileBreaks(numBreaks)

  /** Gives class breaks using a histogram that uses exact counts of integer values.
    *
    * @note This cannot handle counts that are larger than Int.MaxValue, and
    *       should not be used with very large datasets whose counts will overflow.
    *       These histograms can get very large with a wide range of values.
    */
  def classBreaksExactInt(numBreaks: Int): Array[Int] =
    histogramExactInt.quantileBreaks(numBreaks)

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
