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


abstract class MutableHistogram[@specialized (Int, Double) T <: AnyVal] extends Histogram[T] {
  /**
   * Note the occurance of 'item'.
   *
   * The optional parameter 'count' allows histograms to be built more
   * efficiently. Negative counts can be used to remove a particular number
   * of occurances of 'item'.
   */
  def countItem(item: T, count: Int = 1): Unit

  def countItemInt(item: Int, count: Int = 1): Unit

  /**
   * Forget all occurances of 'item'.
   */
  def uncountItem(item: T): Unit

  def update(other: Histogram[T]): Unit

  /**
   * Sets the item to the given count.
   */
  def setItem(item: T, count: Int): Unit

  def quantileBreaks(num: Int): Array[T]
}
