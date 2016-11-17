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

abstract class StatsMultibandTileRDDMethods[K: ClassTag] extends MethodExtensions[RDD[(K, MultibandTile)]] {

  /**
    * Compute the histogram of an RDD of [[MultibandTile]] objects.
    *
    * @return  A [[Histogram[Double]]]
    */
  def histogram(): Array[Histogram[Double]] =
    histogram(StreamingHistogram.DEFAULT_NUM_BUCKETS)

  /**
    * Compute the histogram of an RDD of [[MultibandTile]] objects.
    *
    * @param  numBuckets  The number of buckets that the histogram should have
    * @param  fraction    The fraction of [[MultibandTile]] objects to sample (the default is 100%)
    * @param  seed        The seed of the RNG which determines which tiles to take the histograms of
    * @return             A [[Histogram[Double]]]
    */
  def histogram(numBuckets: Int, fraction: Double = 1.0, seed: Long = 33): Array[Histogram[Double]] =
    (if (fraction >= 1.0) self; else self.sample(withReplacement = false, fraction, seed))
      .map { case (key, tile) => tile.histogramDouble(numBuckets) }
      .reduce { _ zip _  map { case (a, b) => a merge b } }


  /** Gives a histogram that uses exact counts of integer values.
    *
    * @note This cannot handle counts that are larger than Int.MaxValue, and
    *       should not be used with very large datasets whose counts will overflow.
    *       These histograms can get very large with a wide range of values.
    */
  def histogramExactInt: Array[Histogram[Int]] = {
    self
      .map { case (key, tile) => tile.histogram }
      .reduce { _ zip _  map { case (a, b) => a merge b } }
  }

  def classBreaks(numBreaks: Int): Array[Array[Int]] =
    classBreaksDouble(numBreaks).map(_.map(_.toInt))

  def classBreaksDouble(numBreaks: Int): Array[Array[Double]] =
    histogram(numBreaks).map(_.quantileBreaks(numBreaks))

  /** Gives class breaks using a histogram that uses exact counts of integer values.
    *
    * @note This cannot handle counts that are larger than Int.MaxValue, and
    *       should not be used with very large datasets whose counts will overflow.
    *       These histograms can get very large with a wide range of values.
    */
  def classBreaksExactInt(numBreaks: Int): Array[Array[Int]] =
    histogramExactInt.map(_.quantileBreaks(numBreaks))
}
