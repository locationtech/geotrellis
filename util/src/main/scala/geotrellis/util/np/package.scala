/*
 * Copyright 2019 Azavea
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

package geotrellis.util

import spire.syntax.cfor.cfor

package object np {

  /**
    * Return evenly spaced numbers over the specified interval
    * @param min Min bound of the interval, inclusive
    * @param max Max bound of the interval, inclusive
    * @param points Number of samples to generate
    * @return
    */
  def linspace(min: Double, max: Double, points: Int): Array[Double] = {
    val d = new Array[Double](points)
    cfor(0)(_ < points, _ + 1) { i =>
      d(i) = min + i * (max - min) / (points - 1)
    }
    d
  }

  /**
    * Compute percentile at the given breaks using the same algorithm as numpy
    *
    * https://docs.scipy.org/doc/numpy/reference/generated/numpy.percentile.html
    * https://en.wikipedia.org/wiki/Percentile
    *
    * @param data
    * @param pctBreaks
    * @return
    */
  def percentile(data: Array[Double],
                 pctBreaks: Array[Double]): Array[Double] = {
    if (data.nonEmpty) {
      val length = data.length
      val sorted = data.sorted
      if (length == 1) {
        Array(data(0))
      } else {
        pctBreaks.map { break =>
          val n = (break / 100d) * (length - 1d) + 1d
          val k = math.floor(n).toInt
          val d = n - k
          if (k <= 0) {
            sorted(0)
          } else if (k >= length) {
            sorted.last
          } else {
            sorted(k - 1) + d * (sorted(k) - sorted(k - 1))
          }
        }
      }
    } else {
      Array()
    }
  }

  /**
    * Helper to compute percentile at a single break.
    *
    * See [[percentile(data: Array[Double], pctBreaks: Array[Double])]]
    *
    * @param data
    * @param break
    * @return
    */
  def percentile(data: Array[Double], break: Double): Double =
    percentile(data, Array(break))(0)
}
