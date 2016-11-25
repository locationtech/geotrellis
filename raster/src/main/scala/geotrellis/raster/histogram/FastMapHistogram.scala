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

import geotrellis.raster._

import scala.math.{ceil, max, min, abs}
import scala.util.Sorting

import sys.error


/**
  * Companion object for the [[FastMapHistogram]] type.
  */
object FastMapHistogram {
  private final val UNSET: Int = NODATA + 1
  private final val SIZE = 16

  private def buckets(size: Int) = Array.ofDim[Int](size).fill(UNSET)

  private def counts(size: Int) = Array.ofDim[Long](size)

  /**
    * Produce a [[FastMapHistogram]] with the default number of
    * buckets (16).
    */
  def apply() = new FastMapHistogram(SIZE, buckets(SIZE), counts(SIZE), 0, 0)

  /**
    * Produce a [[FastMapHistogram]] with the given number of buckets.
    */
  def apply(size: Int) = new FastMapHistogram(size, buckets(size), counts(size), 0, 0)

  /**
    * Produce a [[FastMapHistogram]] from a [[Tile]].
    */
  def fromTile(r: Tile): FastMapHistogram = {
    val h = FastMapHistogram()
    r.foreach(z => if (isData(z)) h.countItem(z, 1))
    h
  }
}

/**
  * The [[FastMapHistogram]] class.  Quickly compute histograms over
  * integer data.
  */
class FastMapHistogram(_size: Int, _buckets: Array[Int], _counts: Array[Long], _used: Int, _total: Long)
    extends MutableIntHistogram {
  private var size = _size
  private var buckets = _buckets
  private var counts = _counts
  private var used = _used
  private var total = _total

  if (size <= 0) error("size must be > 0")

  // We are reserving this value
  private final val UNSET: Int = NODATA + 1

  // Once our buckets get 60% full, we need to resize
  private final val FACTOR: Double = 0.6

  // Since the underlying implementation uses an array we can only
  // store so many unique values. 1<<30 is the largest power of two
  // that can be allocated as an array. since each key/value pair
  // takes up two slots in our array, the maximum number of those we
  // can store is 1<<29. so that is the maximum size we can allocate
  // for our hash table.
  private final val MAXSIZE: Int = 1<<29

  // Related to the hash table itself
  private var mask = size - 1
  private var limit = (size * FACTOR).toInt

  /**
    * Given item (in the range UNSET + 1 to Int.MaxValue) we return an
    * index into buckets that is either open, or allocated to item.
    *
    * The hashing strategy we use is based on Python's dictionary.
    */
  private final def hashItem(item: Int, mask: Int, bins: Array[Int]): Int = {
    var i = item & 0x7fffffff // need base hashcode to be non-negative.
    var j = i & mask          // need result to be non-negative and even.

    // If we found our own bucket, or an empty bucket, then we're done
    var key = bins(j)
    if (key == UNSET || key == item) return j

    var failsafe = 0

    // We collided with a different item
    var perturb = i
    while (failsafe < 100000000) {
      failsafe += 1
      // I stole this whole perturbation/rehashing strategy from python
      i = (i << 2) + i + perturb + 1
      j = i & mask

      // Similarly, if we found a bucket we can use, we're done
      key = bins(j)
      if (key == UNSET || key == item) return j

      // Adjust perturb. Eventually perturb will zero out.
      perturb >>= 5
    }

    // Should never happen
    error(s"should not happen: item=$item mask=$mask")
    -1
  }

  /**
    * Adjust the histogram so that that it is as if the given value
    * 'item' has been seen 'count' times.
    */
  def setItem(item: Int, count: Long) {
    // We use our hashing strategy to figure out which bucket this
    // item gets.  if the bucket is empty, we're adding the item,
    // whereas if its not we are just increasing the item's count.
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) {
      buckets(i) = item
      counts(i) = count
      used += 1
      if (used > limit) resize()
      total += count
    } else {
      total = total - counts(i) + count
      counts(i) = count
    }
  }

  /**
    * For the value 'item', register the appearance of 'count' more
    * instances of it.
    */
  def countItem(item: Int, count: Long):Unit = {
    // We use our hashing strategy to figure out which bucket this
    // item gets.  if the bucket is empty, we're adding the item,
    // whereas if its not we are just increasing the item's count.
    if (count != 0) {
      val i = hashItem(item, mask, buckets)
      if (buckets(i) == UNSET) {
        buckets(i) = item
        counts(i) = count
        used += 1
        if (used > limit) resize()
      } else {
        counts(i) += count
      }
      total += count
    }
  }

  /**
    * Forget any encounters with the value 'item'.
    */
  def uncountItem(item: Int) {
    // We use our hashing strategy to figure out which bucket this
    // item gets.  if the bucket is empty, we never counted this
    // item. otherwise, we need to remove this value and its counts.
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) return
    total -= counts(i)
    used -= 1
    buckets(i) = UNSET
    counts(i) = 0
  }

  private def resize() {
    // It's important that size always be a power of 2. We grow our
    // hash table by x4 until it starts getting big, at which point we
    // only grow by x2.
    val factor = if (size < 10000) 4 else 2

    // We build the internals of a new hash table first then rehash
    // all our existing keys/values into the new one, at which point
    // we swap out the internals.
    val nextsize = size * factor
    val nextmask = nextsize - 1
    val nextbuckets = Array.ofDim[Int](nextsize).fill(UNSET)
    val nextcounts = Array.ofDim[Long](nextsize)

    // Given the underlying array implementation we can only store so
    // many unique values. given that 1<<30
    if (nextsize > MAXSIZE) error("histogram has exceeded max capacity")

    var i = 0
    while (i < size) {
      val item = buckets(i)
      if (item != UNSET) {
        val j = hashItem(item, nextmask, nextbuckets)
        nextbuckets(j) = item
        nextcounts(j) = counts(i)
      }
      i += 1
    }

    size = nextsize
    mask = nextmask
    buckets = nextbuckets
    counts = nextcounts
    limit = (size * FACTOR).toInt
  }

  def totalCount = total

  /**
    * Return a mutable copy of the present [[FastMapHistogram]].
    */
  def mutable() = new FastMapHistogram(size, buckets.clone(), counts.clone(), used, total)

  /**
    * Return an integer array containing the values seen by this
    * histogram.
    */
  def rawValues() = {
    val keys = Array.ofDim[Int](used)
    var i = 0
    var j = 0
    while (i < size) {
      if (buckets(i) != UNSET) {
        keys(j) = buckets(i)
        j += 1
      }
      i += 1
    }
    keys
  }

  /**
    * Return a sorted integer array of values seen by this histogram.
    */
  def values() = {
    val keys = rawValues()
    Sorting.quickSort(keys)
    keys
  }

  /**
    * Execute the given function on each value seen by the histogram.
    *
    * @param  f  A unit function of one integer parameter
    */
  def foreachValue(f: Int => Unit) {
    var i = 0
    while (i < size) {
      if (buckets(i) != UNSET) f(buckets(i))
      i += 1
    }
  }

  /**
    * The total number of items seen by this histogram.
    */
  def itemCount(item: Int) = {
    val i = hashItem(item, mask, buckets)
    if (buckets(i) == UNSET) 0 else counts(i)
  }

  /**
    * Returns the smallest value seen by the histogram, if it has seen
    * any values.
    */
  def minValue: Option[Int] = {
    var zmin = Int.MaxValue
    var i = 0
    while (i < size) {
      val z = buckets(i)
      if (z != UNSET && z < zmin) zmin = z
      i += 1
    }
    if (zmin != Int.MaxValue)
      Some(zmin)
    else
      None
  }

  /**
    * Returns the largest value seen by the histogram, if it has seen
    * any values.
    */
  def maxValue: Option[Int] = {
    var zmax = Int.MinValue
    var i = 0
    while (i < size) {
      val z = buckets(i)
      if (z != UNSET && z > zmax) zmax = z
      i += 1
    }
    if (zmax != Int.MinValue)
      Some(zmax)
    else
      None
  }

  /**
    * CDF of the distribution.
    */
  def cdf(): Array[(Double, Double)] = {
    val pdf = counts.map({ n => (n / total.toDouble) })

    buckets
      .map({ label => label.toDouble })
      .zip(pdf.scanLeft(0.0)(_ + _).drop(1))
      .toArray
  }

  /**
    * Return the smallest and largest values seen by the histogram, if
    * it has seen any values.
    */
  override def minMaxValues: Option[(Int, Int)] = {
    var zmin = Int.MaxValue
    var zmax = Int.MinValue
    var i = 0
    while (i < size) {
      val z = buckets(i)
      if (z == UNSET) {
      } else if (z < zmin) {
        zmin = z
      } else if (z > zmax) {
        zmax = z
      }
      i += 1
    }
    // there's a small chance that we saw the max first, and zmin consumed it.
    // in that case we need to make sure to set zmax properly.
    if (zmax == Int.MinValue && zmin == Int.MaxValue)
      None
    else if (zmax == Int.MinValue && zmin > zmax)
      Some((zmin, zmin))
    else
      Some((zmin, zmax))
  }

  /**
    * The number of buckets utilized by this [[FastMapHistogram]].
    */
  def bucketCount() = used

  /**
    * The maximum number of buckets this histogram can hold.
    */
  def maxBucketCount: Int = MAXSIZE

  /**
    * Return the sum of this histogram and the given one (the sum is
    * the histogram that would result from seeing all of the values
    * seen by the two antecedent histograms).
    */
  def merge(histogram: Histogram[Int]): Histogram[Int] = {
    val total = FastMapHistogram()

    total.update(this); total.update(histogram)
    total
  }
}
