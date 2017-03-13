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
import geotrellis.util.MethodExtensions

trait StatsTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {

  def averageByKey(): Seq[(K, Tile)] =
    self.groupBy(_._1).mapValues { seq => seq.map(_._2).reduce(_ + _) / seq.size } toSeq

  def histogram(): Histogram[Double] =
    histogram(StreamingHistogram.DEFAULT_NUM_BUCKETS)

  def histogram(numBuckets: Int): Histogram[Double] =
    self
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
