/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.histogram

import geotrellis.raster.summary.Statistics


/**
  * Data object representing a histogram of values.
  */
abstract trait Histogram[@specialized (Int, Double) T <: AnyVal] extends Serializable {
  /**
   * Return the number of occurances for 'item'.
   */
  def itemCount(item: T): Int

  /**
   * Return the total number of occurances for all items.
   */
  def totalCount(): Int

  /**
   * Return the smallest item seen.
   */
  def minValue(): T

  /**
   * Return the largest item seen.
   */
  def maxValue(): T

  /**
   * Return the smallest and largest items seen as a tuple.
   */
  def minMaxValues(): (T, T) = (minValue, maxValue)

  /**
   * Return a mutable copy of this histogram.
   */
  def mutable(): MutableHistogram[T]

  def values(): Array[T]

  def rawValues(): Array[T]

  def foreach(f: (T, Int) => Unit): Unit

  def foreachValue(f: T => Unit): Unit

  def quantileBreaks(num: Int): Array[T]

  def mode(): T

  def median(): T

  def mean(): Double

  def statistics(): Statistics[T]

  def bucketCount(): Int

  def merge(histogram: Histogram[T]): Histogram[T]
}
