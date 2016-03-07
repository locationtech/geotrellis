/*
 * Copyright (c) 2016 Azavea.
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

import geotrellis.raster._
import geotrellis.raster.summary.Statistics
import geotrellis.raster.doubleNODATA
import StreamingHistogram.{BucketType, DeltaType}

import math.{abs, min, max, sqrt}

import java.util.Comparator
import java.util.TreeMap
import scala.collection.JavaConverters._


object StreamingHistogram {
  private type BucketType = (Double, Int)
  private type DeltaType = (Double, BucketType, BucketType)

  private val defaultSize = 80

  def apply(size: Int = defaultSize) = new StreamingHistogram(size, None, None)

  def apply(size: Int, buckets: TreeMap[Double, Int], deltas: TreeMap[DeltaType, Unit]) =
    new StreamingHistogram(size, Some(buckets), Some(deltas))
}

/**
  * Ben-Haim, Yael, and Elad Tom-Tov. "A streaming parallel decision
  * tree algorithm."  The Journal of Machine Learning Research 11
  * (2010): 849-872.
  */
class StreamingHistogram(
  m: Int,
  startingBuckets: Option[TreeMap[Double, Int]],
  startingDeltas: Option[TreeMap[DeltaType, Unit]]
) extends MutableHistogram[Double] {

  class DeltaCompare extends Comparator[DeltaType] {
    def compare(a: DeltaType, b: DeltaType): Int =
      if (a._1 < b._1) -1
      else if (a._1 > b._1) 1
      else {
        if (a._2._1 < b._2._1) -1
        else if (a._2._1 > b._2._1) 1
        else 0
      }
  }

  private val buckets = startingBuckets.getOrElse(new TreeMap[Double, Int])
  private val deltas = startingDeltas.getOrElse(new TreeMap[DeltaType, Unit](new DeltaCompare))

  /**
    * Compute the area of the curve between the two buckets.
    */
  private def computeArea(a: BucketType, b: BucketType): Double = {
    val (value1, count1) = a
    val (value2, count2) = b
    val small = if (count1 >= 0 && count2 >= 0) min(count1, count2); else max(count1, count2)
    val big = if (count1 >= 0 && count2 >= 0) max(count1, count2); else min(count1, count2)
    val width = abs(value1 - value2)
    (width * small) + (0.5 * width * (big - small))
  }

  /**
    * Take two buckets and return their composite.
    */
  private def compose(left: BucketType, right: BucketType): BucketType = {
    val (value1, count1) = left
    val (value2, count2) = right

    if (count1 + count2 != 0)
      ((value1*count1 + value2*count2)/(count1 + count2), (count1 + count2))
    else {
      if (count1 == 0) left
      else if (count2 == 0) right
      else (0.0, 0)
    }
  }

  /**
    * Combine the two closest-together buckets.
    *
    * Before: left ----- middle1 ----- middle2 ----- right
    *
    * After: left ------------- middle ------------- right
    *
    * This function appropriately modifies both the buckets and the
    * deltas.  The deltas on either side of the collapsed pair are
    * removed and replaced with deltas between the mid-point and
    * respective extremes.
    */
  private def combine(): Unit = {
    val delta = deltas.firstKey
    val (_, middle1, middle2) = delta
    val middle = compose(middle1, middle2)
    val left = {
      val entry = buckets.lowerEntry(middle1._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }
    val right = {
      val entry = buckets.higherEntry(middle2._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }

    /* remove middle1 and middle2, as well as the delta between them.*/
    buckets.remove(middle1._1)
    buckets.remove(middle2._1)
    deltas.remove(delta)

    /* Remove delta to the left of the combined buckets */
    if (left != None) {
      val oldDelta = middle1._1 - left.get._1
      deltas.remove((oldDelta, left.get, middle1))
    }

    /* Remove delta to the right of the combined buckets */
    if (right != None) {
      val oldDelta = right.get._1 - middle2._1
      deltas.remove((oldDelta, middle2, right.get))
    }

    /* Add delta covering the whole range */
    if (left != None && right != None) {
      val delta = right.get._1 - left.get._1
      deltas.put((delta, left.get, right.get), Unit)
    }

    /* Add the average of the two combined buckets */
    countItem(middle)
  }

  /**
    * Add a bucket to this histogram.  This can be used to add a new
    * sample to the histogram by letting the bucket-count be equal to
    * one, or it can be used to incrementally merge two histograms.
    */
  private def countItem(b: BucketType): Unit = {
    /* First entry */
    if (buckets.size == 0)
      buckets.put(b._1, b._2)
    /* Duplicate entry */
    else if (buckets.containsKey(b._1)) {
      buckets.put(b._1, buckets.get(b._1) + b._2)
      return
    }
    /* Create new entry */
    else {
      val smaller = {
        val entry = buckets.lowerEntry(b._1)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }
      val larger = {
        val entry = buckets.higherEntry(b._1)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }

      /* Remove delta containing new bucket */
      if (smaller != None && larger != None) {
        val large = larger.get
        val small = smaller.get
        val delta = large._1 - small._1
        deltas.remove((delta, small, large))
      }

      /* Add delta between new bucket and next-largest bucket */
      if (larger != None) {
        val large = larger.get
        val delta = large._1 - b._1
        deltas.put((delta, b, large), Unit)
      }

      /* Add delta between new bucket and next-smallest bucket */
      if (smaller != None) {
        val small = smaller.get
        val delta = b._1 - small._1
        deltas.put((delta, small, b), Unit)
      }
    }

    buckets.put(b._1, b._2)
    if (buckets.size > m) combine()
  }

  /**
    * Additional countItem(|s)(|Int) methods.
    */
  def countItem(item: Double, count: Int = 1): Unit =
    countItem((item, count))
  def countItemInt(item: Int, count: Int = 1): Unit =
    countItem((item.toDouble, count))
  def countItems(items: Seq[BucketType]): Unit =
    items.foreach({ item => countItem(item) })
  def countItems(items: Seq[Double])(implicit dummy: DummyImplicit): Unit =
    items.foreach({ item => countItem((item, 1)) })

  /**
    * Uncount item.
    */
  def uncountItem(item: Double): Unit =
    countItem((item, -1))

  /**
    * Get the (approximate) number of occurances of an item.
    */
  def getItemCount(item: Double): Int = {
    val lo = buckets.lowerEntry(item * 1.0001)
    val hi = buckets.higherEntry(item * 1.0001)
    val raw = {
      if (lo == null && hi == null) 0
      else if (lo == null) {
        val x = item / hi.getKey
        x * hi.getValue
      }
      else if (hi == null) {
        val x = (lo.getKey - item) / lo.getKey
        (1 - x) * lo.getValue
      }
      else {
        val x = (item - lo.getKey) / (hi.getKey - lo.getKey)
        x * (hi.getValue - lo.getValue) + lo.getValue
      }
    }
    return ((raw / getAreaUnderCurve) * getTotalCount).toInt
  }

  /**
    * Make a change to the distribution to approximate changing the
    * value of a particular item.
    */
  def setItem(item: Double, count: Int): Unit = {
    val oldCount = getItemCount(item)
    countItem(item, -oldCount)
    countItem(item, count)
  }

  /**
    * Return an array of bucket values.
    */
  def getValues(): Array[Double] = getBuckets.map(_._1).toArray
  def rawValues(): Array[Double] = getValues

  /**
    * For each bucket ...
    */
  def foreach(f: (Double, Int) => Unit): Unit =
    getBuckets.map({ case(item, count) => f(item, count) })

  /**
    * For each bucket label ...
    */
  def foreachValue(f: Double => Unit): Unit =
    getBuckets.map({ case (item, _) => f(item) })

  /**
    * Generate Statistics.
    */
  def generateStatistics(): Statistics[Double] = {
    val dataCount = getTotalCount
    val mean = getMean
    val median = getMedian
    val mode = getMode
    val ex2 = getBuckets.map({ case(item, count) => item*item*count }).sum / getTotalCount
    val stddev = sqrt(ex2 - mean*mean)
    val zmin = getMinValue
    val zmax = getMaxValue

    Statistics[Double](dataCount, mean, median, mode, stddev, zmin, zmax)
  }

  /**
    * Update this histogram with the entries from another.
    */
  def update(other: Histogram[Double]): Unit = {
    require(other.isInstanceOf[StreamingHistogram])
    this.countItems(other.asInstanceOf[StreamingHistogram].getBuckets)
  }

  def mutable(): StreamingHistogram =
    StreamingHistogram(this.m, this.buckets, this.deltas)

  /**
    * Create a new histogram from this one and another without
    * altering either.
    */
  def +(other: StreamingHistogram): StreamingHistogram = {
    val sh = StreamingHistogram(this.m, this.buckets, this.deltas)
    sh.countItems(other.getBuckets)
    sh
  }

  def merge(other: StreamingHistogram): StreamingHistogram = {
    this + other
  }

  /**
    * Return the approximate mode of the distribution.  This is done
    * by simply returning the label of most populous bucket (so this
    * answer could be really bad).
    */
  def getMode(): Double = {
    if (getTotalCount <= 0) doubleNODATA
    else
      getBuckets.reduce({ (l,r) =>
        if (l._2 > r._2) l; else r
      })._1
  }

  /**
    * Median.
    */
  def getMedian(): Double = getPercentile(0.50)

  /**
    *  Mean.
    */
  def getMean(): Double = {
    val weightedSum =
      getBuckets.foldLeft(0.0)({ (acc,bucket) =>
          acc + (bucket._1 * bucket._2)
        })
    weightedSum / getTotalCount
  }

  /**
    * Return the area under the curve.
    */
  def getAreaUnderCurve(): Double = {
    getBuckets
      .sliding(2)
      .map({ case List(x,y) => computeArea(x,y) })
      .sum
  }

  /**
    * Total number of samples used to build this histogram.
    */
  def getTotalCount(): Int = getBuckets.map(_._2).sum

  /**
    * Get the (approximate) min value.  This is only approximate
    * because the lowest bucket may be a combined one.
    */
  def getMinValue(): Double = {
    val entry = buckets.higherEntry(Double.NegativeInfinity)
    if (entry != null) entry.getKey; else Double.NaN
  }

  /**
    * Get the (approximate) max value.
    */
  def getMaxValue(): Double = {
    val entry = buckets.lowerEntry(Double.PositiveInfinity)
    if (entry != null) entry.getKey; else Double.NaN
  }

  /**
    * This returns a tuple of tuples, where the inner tuples contain a
    * bucket label and its percentile.
    */
  private def getCdfIntervals(): Iterator[((Double, Double), (Double, Double))] = {
    val bs = getBuckets
    val n = getTotalCount
    val ds = bs.map(_._1)
    val pdf = bs.map(_._2.toDouble / n)
    val cdf = pdf.scanLeft(0.0)(_ + _).drop(1)
    val data = ds.zip(cdf).sliding(2)

    data.map({ ab => (ab.head, ab.tail.head) })
  }

  /**
    * Get the (approximate) percentile of this item.
    */
  def getPercentileRanking(item: Double): Double = {
    val data = getCdfIntervals
    val tt = data.dropWhile(_._2._1 <= item).next
    val (d1, pct1) = tt._1
    val (d2, pct2) = tt._2
    val x = (item - d1) / (d2 - d1)

    (1-x)*pct1 + x*pct2
  }

  /**
    * For each q in qs, all between 0 and 1, find a number
    * (approximately) at the qth percentile.
    */
  def getPercentileBreaks(qs: Seq[Double]): Seq[Double] = {
    val data = getCdfIntervals
    qs.map({ q =>
      val tt = data.dropWhile(_._2._2 <= q).next
      val (d1, pct1) = tt._1
      val (d2, pct2) = tt._2
      val x = (q - pct1) / (pct2 - pct1)

      (1-x)*d1 + x*d2
    })
  }

  def getPercentile(q: Double): Double =
    getPercentileBreaks(List(q)).head

  def getQuantileBreaks(num: Int): Array[Double] =
    getPercentileBreaks(List.range(0,num).map(_ / num.toDouble)).toArray

  def getBuckets(): List[BucketType] = buckets.asScala.toList

  def getDeltas(): List[DeltaType] = deltas.keySet.asScala.toList
}
