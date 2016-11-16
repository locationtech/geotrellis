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
import geotrellis.raster.histogram.{Histogram, StreamingHistogram}
import geotrellis.util.MethodExtensions


/**
  * Trait containing [[MultibandTile]] extension methods for summaries.
  */
trait MultibandTileSummaryMethods extends MethodExtensions[MultibandTile] {

  /**
    * Contains several different operations for building a histograms
    * of a raster.
    *
    * @note     Tiles with a double type (FloatConstantNoDataCellType, DoubleConstantNoDataCellType) will have their values
    *           rounded to integers when making the Histogram.
    */
  def histogram: Array[Histogram[Int]] =
    self.bands.map(_.histogram).toArray

  /**
    * Create a histogram from double values in a raster.
    */
  def histogramDouble(): Array[Histogram[Double]] =
    histogramDouble(StreamingHistogram.DEFAULT_NUM_BUCKETS)

  /**
    * Create a histogram from double values in a raster.
    */
  def histogramDouble(numBuckets: Int): Array[Histogram[Double]] =
    self.bands.map(StreamingHistogram.fromTile(_, numBuckets)).toArray

  /**
    * Generate quantile class breaks for a given raster.
    */
  def classBreaks(numBreaks: Int): Array[Array[Int]] =
    self.bands.map(_.classBreaks(numBreaks)).toArray

  /**
    * Generate quantile class breaks for a given raster.
    */
  def classBreaksDouble(numBreaks: Int): Array[Array[Double]] =
    self.bands.map(_.classBreaksDouble(numBreaks)).toArray

  /**
    * Determine statistical data for the given histogram.
    *
    * This includes mean, median, mode, stddev, and min and max values.
    */
  def statistics: Array[Option[Statistics[Int]]] =
    self.bands.map(_.histogram.statistics()).toArray

  /**
    * Determine statistical data for the given histogram.
    *
    * This includes mean, median, mode, stddev, and min and max values.
    */
  def statisticsDouble: Array[Option[Statistics[Double]]] =
    self.bands.map(_.histogramDouble().statistics()).toArray

  /**
    * Calculate a raster in which each value is set to the standard deviation of that cell's value.
    *
    * @return        MultibandTile of IntConstantNoDataCellType data
    * @note          Currently only supports working with integer types. If you pass in a MultibandTile
    *                with double type data (FloatConstantNoDataCellType, DoubleConstantNoDataCellType) the values will be rounded to
    *                Ints.
    */
  def standardDeviations(factor: Double = 1.0): MultibandTile =
    self.mapBands { (band, tile) => tile.standardDeviations(factor) }
}
