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

package geotrellis.raster.statistics

import geotrellis.raster._
import math.{abs, ceil, min, max, sqrt}

object ArrayHistogram {
  def apply(size: Int) = new ArrayHistogram(Array.ofDim[Int](size).fill(0), 0)

  def apply(counts: Array[Int], total: Int) = new ArrayHistogram(counts, total)

  def fromTile(r: Tile, n: Int): ArrayHistogram = {
    val h = ArrayHistogram(n)
    r.foreach(z => if (isData(z)) h.countItem(z, 1))
    h
  }

  def fromHistograms(hs: List[Histogram], n: Int): ArrayHistogram = {
    val total = ArrayHistogram(n)
    hs.foreach(h => total.update(h))
    total
  }
}

// TODO: can currently only handle non-negative integers

/**
  * Data object representing a histogram that uses an array for internal storage. 
  */
class ArrayHistogram(val counts: Array[Int], var total: Int) 
    extends MutableHistogram {
  def size = counts.length

  def getTotalCount = total

  def mutable() = ArrayHistogram(counts.clone, total)

  def foreachValue(f: Int => Unit) {
    var i = 0
    val len = counts.length
    while (i < len) {
      val z = counts(i)
      if (z > 0) f(i)
      i += 1
    }
  }

  def getValues() = rawValues()

  def rawValues() = (0 until counts.length).filter(counts(_) > 0).toArray

  def setItem(i: Int, count: Int) {
    total = total - counts(i) + count
    counts(i) = count
  }

  def uncountItem(i: Int) {
    total -= counts(i)
    counts(i) = 0
  }

  def countItem(i: Int, count: Int=1) {
    total += count
    counts(i) += count
  }

  def getItemCount(i: Int) = counts(i)

  // REFACTOR: use Option
  def getMinValue: Int = {
    var i = 0
    val limit = counts.length
    while (i < limit) {
      if (counts(i) > 0) return i
      i += 1
    }
    return Int.MaxValue
  }

  // REFACTOR: use Option
  def getMaxValue: Int = {
    var i = counts.length - 1
    while (i >= 0) {
      if (counts(i) > 0) return i
      i -= 1
    }
    return Int.MinValue
  }
}
