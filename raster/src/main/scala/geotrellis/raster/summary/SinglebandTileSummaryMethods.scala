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

package geotrellis.raster.summary

import geotrellis.raster._
import geotrellis.raster.histogram._
import geotrellis.util.MethodExtensions


/**
  * Trait containing [[Tile]] extension methods for summaries.
  */
trait SinglebandTileSummaryMethods extends MethodExtensions[Tile] {

  /**
    * Contains several different operations for building a histograms
    * of a raster.
    *
    * @note     Tiles with a double type (FloatConstantNoDataCellType, DoubleConstantNoDataCellType) will have their values
    *           rounded to integers when making the Histogram.
    */
  def histogram: Histogram[Int] =
    FastMapHistogram.fromTile(self)

  /**
    * Create a histogram from double values in a raster.
    */
  def histogramDouble(): Histogram[Double] =
    histogramDouble(StreamingHistogram.DEFAULT_NUM_BUCKETS)

  /**
    * Create a histogram from double values in a raster.
    */
  def histogramDouble(numBuckets: Int): Histogram[Double] =
    StreamingHistogram.fromTile(self, numBuckets)

  /**
    * Generate quantile class breaks for a given raster.
    */
  def classBreaks(numBreaks: Int): Array[Int] =
    histogram.quantileBreaks(numBreaks)

  /**
    * Generate quantile class breaks for a given raster.
    */
  def classBreaksDouble(numBreaks: Int): Array[Double] =
    histogramDouble.quantileBreaks(numBreaks)

  /**
    * Determine statistical data for the given histogram.
    *
    * This includes mean, median, mode, stddev, and min and max values.
    */
  def statistics: Option[Statistics[Int]] = histogram.statistics

  /**
    * Determine statistical data for the given histogram.
    *
    * This includes mean, median, mode, stddev, and min and max values.
    */
  def statisticsDouble: Option[Statistics[Double]] = histogramDouble.statistics

  /**
    * Calculate a raster in which each value is set to the standard
    * deviation of that cell's value.
    *
    * @return        Tile of IntConstantNoDataCellType data
    *
    * @note          Currently only supports working with integer types. If you pass in a Tile
    *                with double type data (FloatConstantNoDataCellType, DoubleConstantNoDataCellType) the values will be rounded to
    *                Ints.
    */
  def standardDeviations(factor: Double = 1.0): Tile = {
    require(statistics.nonEmpty)
    val Statistics(_, mean, _, _, stddev, _, _) = statistics.get

    val indata = self.toArray
    val len = indata.length
    val result = Array.ofDim[Int](len)

    var i = 0
    while (i < len) {
      val delta = indata(i) - mean
      result(i) = (delta * factor / stddev).toInt
      i += 1
    }

    ArrayTile(result, self.cols, self.rows)
  }
}
