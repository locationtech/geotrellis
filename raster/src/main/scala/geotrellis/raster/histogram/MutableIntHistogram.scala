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

import math.{abs, round, sqrt}


/**
  * All mutable integer histograms are derived from this class.
  */
abstract class MutableIntHistogram extends MutableHistogram[Int] with IntHistogram {

  /**
    * Note the occurance of 'item'.
    *
    * The optional parameter 'count' allows histograms to be built
    * more efficiently. Negative counts can be used to remove a
    * particular number of occurances of 'item'.
    */
  def countItemInt(item: Int, count: Long): Unit = countItem(item, count)

  /**
    * Update this histogram with the entries from the other one.
    */
  def update(other: Histogram[Int]): Unit = {
    other.foreach((z, count) => countItem(z, count))
  }

  /**
    * Return 'num' evenly spaced Doubles from 0.0 to 1.0.
    */
  private def evenQuantiles(num: Int) = (1 to num).map(_.toDouble / num).toArray

  /**
    * This is a heuristic used by quantileBreaks, which mutates the
    * histogram.
    */
  private def normalizeExtremeValues(num: Int, cutoff: Int): Histogram[Int] = {
    // see how many (if any) extreme values we have, and store their indices
    val localValue: Array[Int] = values()
    val vLen = localValue.length

    val eItems: List[Int] = localValue.foldLeft(Nil: List[Int]) {
      (is, i) => if (itemCount(i) > cutoff) i :: is else is
    }
    val eLen = eItems.length

    // if we don't have extreme values we're done
    if (eLen == 0) return this

    val h = mutable()

    // if we only have extreme values, just set all histogram counts to 1.
    if (eLen == vLen) {
      eItems.foreach(item => h.setItem(item, 1))
      return h
    }

    // ok, so we want extreme values to each get exactly one bucket after
    // normalization. we will assign each of our extreme indices the same
    // value, which will be our new target bucket size. to do this, we have to
    // take into account the "new total" (consisting of all our non-extreme
    // values plus the "new" extreme values). here is an equation that might
    // help get the idea across:
    //
    // T: the total of all "non-extreme" values added together
    // Q: the number of quantiles we want
    // E: the number of extreme values we have
    // X: our goal, an extreme value which we will assign into the histogram
    //    for the extreme indices which *also* will correspond to our new
    //    bucket size
    //
    // X             = (T + E * X) / Q
    // X * Q         = (T + E * X)
    // X * Q - X * E = T
    // X * (Q - E)   = T
    // X             = T / (Q - E)
    val eSubtotal: Long = eItems.foldLeft(0L)((t, i) => t + h.itemCount(i))
    val oSubtotal: Long = h.totalCount - eSubtotal
    var eValue: Long = oSubtotal / (num - eLen)

    eItems.foreach(i => h.setItem(i, eValue))
    h
  }

  /**
    * Compute the quantile breaks of the histogram, where the latter
    * are evenly spaced in 'num' increments starting at zero percent.
    */
  def quantileBreaks(num: Int): Array[Int] = {
    // first, we create a list of percentages to use, along with determining
    // how many cells should fit in one "ideal" quantile bucket.
    val quantiles: Array[Double] = evenQuantiles(num)
    val size: Int = (quantiles(0) * totalCount).toInt

    // then we need to make a copy of ourself to do some preprocessing on to
    // remove extreme values. an extreme value is one that would automatically
    // overflow any bucket it is in (even alone). if size=100, and there are
    // 200 cells with the value "1" then 1 would be an extreme value.
    val h: Histogram[Int] = normalizeExtremeValues(num, size)

    // now we'll store some data about the histogram, our quantiles, etc, for
    // future use and fast access.
    val total    = h.totalCount
    val limits   = quantiles.map(_ * total)
    val maxValue = h.maxValue

    // this is the array of breaks we will return
    val breaks = Array.ofDim[Int](quantiles.length)

    // the quantile we're currently working on
    var qIndex = 0

    // the value we're currently working on
    var j = 0
    val localValue = values()

    // the current total of all previous values we've seen
    var currTotal = 0L

    // we're going to move incrementally through the values while comparing
    // a running total against our current quantile (qIndex). we know that the
    // last break is "everything else" so we stop when we reach that one.
    while (qIndex < breaks.length && j < values.length) {
      val i = localValue(j)
      val count = h.itemCount(i)
      val newTotal = currTotal + count

      if (count == 0) {
      } else if (newTotal > limits(qIndex)) {
        if (abs(limits(qIndex) - currTotal) > abs(limits(qIndex) - newTotal)) {
          // in this case localValue(j) is closer than localValue(j - 1)
          breaks(qIndex) = i
        } else if(j > 0) {
          // in this case localValue(j - 1) is closer, did we already use it?
          if (qIndex > 0 && breaks(qIndex - 1) == localValue(j - 1)) {
            // yes, so now use localValue(j)
            breaks(qIndex) = localValue(j)
          } else {
            // no, so use localValue(j - 1)
            breaks(qIndex) = localValue(j - 1)
          }
        } else {
          // in this case j == 0 so there is no localValue(j - 1)
          breaks(qIndex) = i
        }

        qIndex += 1
      }

      currTotal = newTotal
      j += 1
    }

    // put the maximum value at the end
    if (maxValue.nonEmpty && qIndex < breaks.length && (qIndex == 0 || breaks(qIndex - 1) < maxValue.get)) {
      breaks(qIndex) = maxValue.get
      qIndex += 1
    }

    // figure out which breaks got filled, and only return those
    breaks.slice(0, qIndex)
  }
}
