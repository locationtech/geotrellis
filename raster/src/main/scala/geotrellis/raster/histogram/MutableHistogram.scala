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

package geotrellis.raster.histogram


/**
  * All mutable histograms are derived from this type.
  */
abstract class MutableHistogram[@specialized (Int, Double) T <: AnyVal] extends Histogram[T] {

  /**
    * Note the occurance of 'item'.
    */
  def countItem(item: T): Unit = countItem(item, 1L)

  /**
    * Note the occurance of 'item'.
    *
    * The optional parameter 'count' allows histograms to be built
    * more efficiently. Negative counts can be used to remove a
    * particular number of occurances of 'item'.
    */
  def countItem(item: T, count: Long): Unit

  /**
    * Note the occurance of 'item'.
    */
  def countItemInt(item: Int): Unit = countItemInt(item, 1L)

  /**
    * Note the occurance of 'item'.
    *
    * The optional parameter 'count' allows histograms to be built
    * more efficiently. Negative counts can be used to remove a
    * particular number of occurances of 'item'.
    */
  def countItemInt(item: Int, count: Long): Unit

  /**
   * Forget all occurances of 'item'.
   */
  def uncountItem(item: T): Unit

  /**
    * Update this histogram with the entries from the other one.
    */
  def update(other: Histogram[T]): Unit

  /**
   * Sets the item to the given count.
   */
  def setItem(item: T, count: Long): Unit

  /**
    * Compute the quantile breaks of the histogram, where the latter
    * are evenly spaced in 'num' increments starting at zero percent.
    */
  def quantileBreaks(num: Int): Array[T]
}
