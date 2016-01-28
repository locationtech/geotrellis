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
import geotrellis.raster.op.stats.Statistics

import java.util.TreeMap
import java.util.Comparator
import scala.collection.JavaConverters._
import StreamingHistogram.{BucketType, DeltaType}


object StreamingHistogram {
  private type BucketType = (Double, Int)
  private type DeltaType = (Double, BucketType, BucketType)

  def apply(m: Int) = new StreamingHistogram(m, None, None)

  def apply(m: Int, buckets: TreeMap[Double, Int], deltas: TreeMap[DeltaType, Unit]) =
    new StreamingHistogram(m, Some(buckets), Some(deltas))
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

  /* The number of samples represented by this histogram */
  var n = buckets.asScala.map(_._2).sum

  /**
    * Take two buckets and return their composite.
    */
  private def merge(left: BucketType, right: BucketType): BucketType = {
    val (value1, count1) = left
    val (value2, count2) = right
    ((value1*count1 + value2*count2)/(count1 + count2), (count1 + count2))
  }

  /**
    * Merge the two closest-together buckets.
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
  private def merge(): Unit = {
    val delta = deltas.firstKey
    val (_, middle1, middle2) = delta
    val middle = merge(middle1, middle2)
    val left = {
      val entry = buckets.lowerEntry(middle1._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }
    val right = {
      val entry = buckets.higherEntry(middle2._1)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }

    /* remove delta between middle1 and middle2 */
    deltas.remove(delta)

    /* Replace delta to the left the merged buckets */
    if (left != None) {
      val other = left.get
      val oldDelta = middle1._1 - other._1
      val newDelta = middle._1 - other._1
      deltas.remove((oldDelta, other, middle1))
      deltas.put((newDelta, other, middle), Unit)
    }

    /* Replace delta to the right the merged buckets */
    if (right != None) {
      val other = right.get
      val oldDelta = other._1 - middle2._1
      val newDelta = other._1 - middle._1
      deltas.remove((oldDelta, middle2, other))
      deltas.put((newDelta, middle, other), Unit)
    }

    /* Replace merged buckets with their average */
    buckets.remove(middle1._1)
    buckets.remove(middle2._1)
    buckets.put(middle._1, middle._2)
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
    else if (buckets.containsKey(b._1))
      buckets.put(b._1, buckets.get(b._1) + b._2)
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

    n += b._2
    buckets.put(b._1, b._2)
    if (buckets.size > m) merge
  }

  /**
    * Additional count(|Int)Item(|s) methods.
    */
  def countItem(item: Double, count: Int = 1): Unit =
    countItem((item, count))
  def countIntItem(item: Int, count: Int = 1): Unit =
    countItem((item.toDouble, count))
  def countItems(items: Seq[BucketType]): Unit =
    items.foreach({ item => countItem(item) })
  def countItems(items: Seq[Double])(implicit dummy: DummyImplicit): Unit =
    items.foreach({ item => countItem((item, 1)) })

  /**
    * Unimplementable in principle.
    */
  def uncountItem(item: Double): Unit = ???
  def setItem(item: Double, count: Int): Unit = ???
  def getValues(): Array[Double] = ???
  def rawValues(): Array[Double] = ???
  def foreach(f: (Double, Int) => Unit): Unit = ???
  def foreachValue(f: Double => Unit): Unit = ???
  def getItemCount(item: Double): Int = ???

  /**
    * Generate Statistics.
    */
  def generateStatistics(): Statistics[Double] = {
    val dataCount = getTotalCount
    val mean = getMean
    val median = getMedian
    val mode = getMode
    val stddev: Double = ???
    val zmin = getMinValue
    val zmax = getMaxValue

    Statistics[Double](dataCount, mean, median, mode, stddev, zmin, zmax)
  }

  /**
    * Update this histogram with the entries from another.
    */
  def update(other: Histogram[Double]): Unit = {
    if (other.isInstanceOf[StreamingHistogram])
      this.countItems(other.asInstanceOf[StreamingHistogram].getBuckets)
    else
      ???
  }

  def mutable(): StreamingHistogram =
    StreamingHistogram(this.m, this.buckets, this.deltas)

  /**
    * Combine operator: create a new histogram from this one and
    * another without altering either.
    */
  def +(other: StreamingHistogram): StreamingHistogram = {
    val sh = StreamingHistogram(this.m, this.buckets, this.deltas)
    sh.countItems(other.getBuckets)
    sh
  }

  /**
    * Return the approximate mode of the distribution.  This is done
    * by simply returning the most populous bucket (so this answer
    * could be really bad).
    */
  def getMode(): Double = {
    buckets.asScala.reduce({ (l,r) =>
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
      buckets
        .asScala.foldLeft(0.0)({ (acc,bucket) =>
          acc + (bucket._1 * bucket._2)
        })
    weightedSum / getTotalCount
  }

  /**
    * Total number of samples used to build this histogram.
    */
  def getTotalCount(): Int = n

  /**
    * Get the (approximate) min value.  This is only approximate
    * because the lowest bucket may be a composite one.
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
    val scalaBuckets = buckets.asScala
    val ds = scalaBuckets.map(_._1)
    val pdf = scalaBuckets.map(_._2.toDouble / n)
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
    val x = (item - d1) / (item - d2)
    ((x*pct2) + (1-x)*pct1)
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
      ((x*d2) + (1-x)*d1)
    })
  }

  def getPercentile(q: Double): Double =
    getPercentileBreaks(List(q)).head

  def getQuantileBreaks(num: Int): Array[Double] =
    getPercentileBreaks(List.range(0,num).map(_ / num.toDouble)).toArray

  def getBuckets(): List[BucketType] = buckets.asScala.toList

  def getDeltas(): List[DeltaType] = deltas.keySet.asScala.toList
}
