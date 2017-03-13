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

import geotrellis.raster.NODATA
import geotrellis.raster.summary.Statistics

import math.{abs, round, sqrt}
import spire.syntax.cfor._


object IntHistogram {
  def apply() = FastMapHistogram()
  def apply(size: Int) = FastMapHistogram(size)
}

/**
  * The base trait for all integer-containing histograms.
  */
abstract trait IntHistogram extends Histogram[Int] {

  /**
    * Execute the given function on the value and count of each bucket
    * in the histogram.
    */
  def foreach(f: (Int, Long) => Unit): Unit = {
    values.foreach(z => f(z, itemCount(z)))
  }

  /**
    * Compute the mode of the distribution represented by the
    * histogram.
    */
  def mode(): Option[Int] = {
    if(totalCount == 0) { return None }
    val localValues = values()
    var mode = localValues(0)
    var count = itemCount(mode)
    val len = localValues.length
    cfor(1)(_ < len, _ + 1) { i =>
      val z = localValues(i)
      val c = itemCount(z)
      if (c > count) {
        count = c
        mode = z
      }
    }
    Some(mode)
  }

  /**
    * Compute the median of the distribution represented by the
    * histogram.
    */
  def median(): Option[Int] = {
    if (totalCount == 0) {
      None
    } else {
      val localValues = values()
      val middle: Long = totalCount() / 2
      var total = 0L
      var i = 0
      while (total <= middle) {
        total += itemCount(localValues(i))
        i += 1
      }
      Some(localValues(i-1))
    }
  }

  /**
    * Compute the mean of the distribution represented by the
    * histogram.
    */
  def mean(): Option[Double] = {
    if(totalCount == 0) { return None }

    val localValues = rawValues()
    var mean = 0.0
    var total = 0.0
    val len = localValues.length

    cfor(0)(_ < len, _ + 1) { i =>
      val value = localValues(i)
      val count = itemCount(value)
      val delta = value - mean
      total += count
      mean += (count * delta) / total
    }
    Some(mean)
  }

  /**
    * Return a statistics object for the distribution represented by
    * the histogram.  Contains among other things: mean, mode, median,
    * and so-forth.
    */
  def statistics() = {
    val localValues = values()
    if (localValues.length == 0) {
      None
    } else {

      var dataCount: Long = 0

      var mode = 0
      var modeCount = 0L

      var mean = 0.0
      var total = 0L

      var median = 0
      var needMedian = true
      val limit = totalCount() / 2

      val len = localValues.length

      cfor(0)(_ < len, _ + 1) { i =>
        val value = localValues(i)
        val count = itemCount(value)
        dataCount = dataCount + count
        if (count != 0) {
          // update the mode
          if (count > modeCount) {
            mode = value
            modeCount = count
          }

          // update the mean
          val delta = value - mean
          total += count
          mean += (count * delta) / total

          // update median if needed
          if (needMedian && total > limit) {
            median = localValues(i)
            needMedian = false
          }
        }
      }

      // find the min value
      val zmin = localValues(0)

      // find the max value
      val zmax = localValues(len - 1)

      // find stddev
      total = 0
      var mean2 = 0.0
      cfor(0)(_ < len, _ + 1) { i =>
        val value = localValues(i)
        val count = itemCount(value)

        if (count > 0) {
          val x = value - mean
          val y = x * x

          val delta = y - mean2
          total += count
          mean2 += (count * delta) / total
        }
      }
      val stddev = sqrt(mean2)

      Some(Statistics[Int](dataCount, mean, median, mode, stddev, zmin, zmax))
    }
  }
}
