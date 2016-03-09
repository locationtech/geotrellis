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

import math.{abs, exp, min, max, sqrt}

import java.util.Comparator
import java.util.TreeMap
import scala.collection.JavaConverters._


object StreamingHistogram {
  private type BucketType = (Double, Int)
  private type DeltaType = (Double, BucketType, BucketType)

  private val defaultSize = 80

  def apply(size: Int = defaultSize) =
    new StreamingHistogram(size, None, None, Double.PositiveInfinity, Double.NegativeInfinity)

  def apply(
    size: Int,
    minimumSeen: Double,
    maximumSeen: Double
  ) =
    new StreamingHistogram(size, None, None, minimumSeen, maximumSeen)

  def fromTile(r: Tile): StreamingHistogram = {
    val h = StreamingHistogram()
    r.foreach(z => if (isData(z)) h.countItem(z, 1))
    h
  }
}

/**
  * Ben-Haim, Yael, and Elad Tom-Tov. "A streaming parallel decision
  * tree algorithm."  The Journal of Machine Learning Research 11
  * (2010): 849-872.
  */
class StreamingHistogram(
  m: Int,
  startingBuckets: Option[TreeMap[Double, Int]],
  startingDeltas: Option[TreeMap[DeltaType, Unit]],
  minimum: Double = Double.PositiveInfinity,
  maximum: Double = Double.NegativeInfinity
) extends MutableHistogram[Double] {

  class DeltaCompare extends Comparator[DeltaType] with Serializable {
    def compare(a: DeltaType, b: DeltaType): Int =
      if (a._1 < b._1) -1
      else if (a._1 > b._1) 1
      else {
        if (a._2._1 < b._2._1) -1
        else if (a._2._1 > b._2._1) 1
        else 0
      }
  }

  private var _min = minimum
  private var _max = maximum
  private val _buckets = startingBuckets.getOrElse(new TreeMap[Double, Int])
  private val _deltas = startingDeltas.getOrElse(new TreeMap[DeltaType, Unit](new DeltaCompare))

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
    val delta = _deltas.firstKey
    val (_, middle1, middle2) = delta
    val middle = compose(middle1, middle2)
    val left = {
      val entry = _buckets.lowerEntry(middle1._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }
    val right = {
      val entry = _buckets.higherEntry(middle2._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }

    /* remove middle1 and middle2, as well as the delta between them.*/
    _buckets.remove(middle1._1)
    _buckets.remove(middle2._1)
    _deltas.remove(delta)

    /* Remove delta to the left of the combined buckets */
    if (left != None) {
      val oldDelta = middle1._1 - left.get._1
      _deltas.remove((oldDelta, left.get, middle1))
    }

    /* Remove delta to the right of the combined buckets */
    if (right != None) {
      val oldDelta = right.get._1 - middle2._1
      _deltas.remove((oldDelta, middle2, right.get))
    }

    /* Add delta covering the whole range */
    if (left != None && right != None) {
      val delta = right.get._1 - left.get._1
      _deltas.put((delta, left.get, right.get), Unit)
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
    if (b._1 < _min && b._2 != 0) _min = b._1
    if (b._1 > _max && b._2 != 0) _max = b._2

    /* First entry */
    if (_buckets.size == 0)
      _buckets.put(b._1, b._2)
    /* Duplicate entry */
    else if (_buckets.containsKey(b._1)) {
      _buckets.put(b._1, _buckets.get(b._1) + b._2)
      return
    }
    /* Create new entry */
    else {
      val smaller = {
        val entry = _buckets.lowerEntry(b._1)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }
      val larger = {
        val entry = _buckets.higherEntry(b._1)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }

      /* Remove delta containing new bucket */
      if (smaller != None && larger != None) {
        val large = larger.get
        val small = smaller.get
        val delta = large._1 - small._1
        _deltas.remove((delta, small, large))
      }

      /* Add delta between new bucket and next-largest bucket */
      if (larger != None) {
        val large = larger.get
        val delta = large._1 - b._1
        _deltas.put((delta, b, large), Unit)
      }

      /* Add delta between new bucket and next-smallest bucket */
      if (smaller != None) {
        val small = smaller.get
        val delta = b._1 - small._1
        _deltas.put((delta, small, b), Unit)
      }
    }

    _buckets.put(b._1, b._2)
    if (_buckets.size > m) combine()
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
    *
    * Note: _min and _max are not changed by this.
    */
  def uncountItem(item: Double): Unit =
    countItem((item, -1))

  /**
    * Get the (approximate) number of occurances of an item.
    */
  def itemCount(item: Double): Int = {
    if (bucketCount() == 0) 0
    else if (bucketCount() == 1) {
      val (item2, count) = buckets().head
      count / exp(abs(7 * (item2 - item))).toInt
    }
    else {
      val lo = _buckets.lowerEntry(item * 1.0001)
      val hi = _buckets.higherEntry(item * 1.0001)
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
      if (areaUnderCurve != 0) ((raw / areaUnderCurve) * totalCount).toInt
      else 0
    }
  }

  /**
    * Make a change to the distribution to approximate changing the
    * value of a particular item.
    *
    * Note: _min and _max are not changed by this.
    */
  def setItem(item: Double, count: Int): Unit = {
    val oldCount = itemCount(item)
    countItem(item, -oldCount)
    countItem(item, count)
  }

  /**
    * Return an array of bucket values.
    */
  def values(): Array[Double] = buckets.map(_._1).toArray
  def rawValues(): Array[Double] = values()

  /**
    * For each bucket ...
    */
  def foreach(f: (Double, Int) => Unit): Unit =
    buckets.map({ case(item, count) => f(item, count) })

  /**
    * For each bucket label ...
    */
  def foreachValue(f: Double => Unit): Unit =
    buckets.map({ case (item, _) => f(item) })

  /**
    * Generate Statistics.
    */
  def statistics(): Statistics[Double] = {
    val dataCount = totalCount
    val localMean = mean()
    val localMedian = median()
    val localMode = mode
    val ex2 = buckets.map({ case(item, count) => item*item*count }).sum / totalCount
    val stddev = sqrt(ex2 - localMean * localMean)
    val zmin = minValue.getOrElse(this._min)
    val zmax = maxValue.getOrElse(this._max)

    Statistics[Double](dataCount, localMean, localMedian, localMode, stddev, zmin, zmax)
  }

  /**
    * Update this histogram with the entries from another.
    */
  def update(other: Histogram[Double]): Unit =
    other.foreach({ case (item, count) => this.countItem((item, count)) })

  def mutable(): StreamingHistogram = {
    val sh = StreamingHistogram(this.m, this._min, this._max)
    sh.countItems(this.buckets)
    sh
  }

  /**
    * Create a new histogram from this one and another without
    * altering either.
    */
  def +(other: StreamingHistogram): StreamingHistogram = {
    val sh = StreamingHistogram(this.m, this._min, this._max)
    sh.countItems(this.buckets)
    sh.countItems(other.buckets)
    sh
  }

  def bucketCount():Int = _buckets.size

  def merge(histogram: Histogram[Double]): Histogram[Double] = {
    val sh = StreamingHistogram(this.m, this._min, this._max)
    sh.countItems(this.buckets)
    histogram.foreach({ (item: Double, count: Int) => sh.countItem((item, count)) })
    sh
  }

  /**
    * Return the approximate mode of the distribution.  This is done
    * by simply returning the label of most populous bucket (so this
    * answer could be really bad).
    */
  def mode(): Double = {
    if (totalCount <= 0) doubleNODATA
    else
      buckets.reduce({ (l,r) => if (l._2 > r._2) l; else r })._1
  }

  /**
    * Median.
    */
  def median(): Double = percentile(0.50)

  /**
    *  Mean.
    */
  def mean(): Double = {
    val weightedSum =
      buckets.foldLeft(0.0)({ (acc,bucket) => acc + (bucket._1 * bucket._2) })
    weightedSum / totalCount
  }

  /**
    * Return the area under the curve.
    */
  def areaUnderCurve(): Double = {
    buckets()
      .sliding(2)
      .map({
        case List(x,y) => computeArea(x,y)
        case List(_) => 0.0
      })
      .sum
  }

  /**
    * Total number of samples used to build this histogram.
    */
  def totalCount(): Int = buckets.map(_._2).sum

  /**
    * Get the (approximate) min value.  This is only approximate
    * because the lowest bucket may be a combined one.
    */
  def minValue(): Option[Double] = {
    val entry = _buckets.higherEntry(Double.NegativeInfinity)
    if (entry != null) Some(entry.getKey); else None
  }

  /**
    * Get the (approximate) max value.
    */
  def maxValue(): Option[Double] = {
    val entry = _buckets.lowerEntry(Double.PositiveInfinity)
    if (entry != null) Some(entry.getKey); else None
  }

  /**
    * This returns a tuple of tuples, where the inner tuples contain a
    * bucket label and its percentile.
    */
  private def cdfIntervals(): Iterator[((Double, Double), (Double, Double))] = {
    val bs = buckets
    val n = totalCount
    val ds = bs.map(_._1)
    val pdf = bs.map(_._2.toDouble / n)
    val cdf = pdf.scanLeft(0.0)(_ + _).drop(1)
    val data = ds.zip(cdf).sliding(2)

    data.map({ ab => (ab.head, ab.tail.head) })
  }

  /**
    * Get the (approximate) percentile of this item.
    */
  def percentileRanking(item: Double): Double = {
    val data = cdfIntervals
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
  def percentileBreaks(qs: Seq[Double]): Seq[Double] = {
    val data = cdfIntervals
    qs.map({ q =>
      if (q == 0.0) minValue().getOrElse(Double.NegativeInfinity)
      else if (q == 1.0) maxValue().getOrElse(Double.PositiveInfinity)
      else {
        val tt = data.dropWhile(_._2._2 <= q).next
        val (d1, pct1) = tt._1
        val (d2, pct2) = tt._2
        val x = (q - pct1) / (pct2 - pct1)

        (1-x)*d1 + x*d2
      }
    })
  }

  def percentile(q: Double): Double =
    percentileBreaks(List(q)).head

  def quantileBreaks(num: Int): Array[Double] =
    percentileBreaks(List.range(0,num).map(_ / num.toDouble)).toArray

  def buckets(): List[BucketType] = _buckets.asScala.toList

  def deltas(): List[DeltaType] = _deltas.keySet.asScala.toList
}
