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
import geotrellis.raster.summary.Statistics
import geotrellis.raster.doubleNODATA
import StreamingHistogram.{Bucket, Delta, DeltaCompare}

import math.{abs, exp, max, min, sqrt}
import java.util.Comparator
import java.util.TreeMap

import cats.Monoid

import scala.collection.JavaConverters._
import scala.collection.mutable.{ListBuffer => MutableListBuffer}


object StreamingHistogram {

  implicit val streamingHistogramMonoid: Monoid[StreamingHistogram] =
    new Monoid[StreamingHistogram] {
      def empty: StreamingHistogram = StreamingHistogram()
      def combine(x: StreamingHistogram, y: StreamingHistogram) = x.merge(y)
    }

  case class Bucket(label: Double, count: Long) {
    def _1 = label
    def _2 = count
  }
  object Bucket { implicit def tupToBucket(tup: (Double, Long)): Bucket = Bucket(tup._1, tup._2) }

  case class Delta(distance: Double, left: Bucket, right: Bucket) {
    def _1 = distance
    def _2 = left
    def _3 = right
  }
  object Delta { implicit def tupToDelta(tup: (Double, Bucket, Bucket)): Delta = Delta(tup._1, tup._2, tup._3) }

  class DeltaCompare extends Comparator[Delta] with Serializable {
    def compare(a: Delta, b: Delta): Int =
      if (a.distance < b.distance) -1
      else if (a.distance > b.distance) 1
      else {
        if (a.left.label < b.left.label) -1
        else if (a.left.label > b.left.label) 1
        else 0
      }
  }

  def DEFAULT_NUM_BUCKETS = 80

  def apply(size: Int = DEFAULT_NUM_BUCKETS) =
    new StreamingHistogram(size, Double.PositiveInfinity, Double.NegativeInfinity)

  def apply(
    size: Int,
    minimumSeen: Double,
    maximumSeen: Double
  ): StreamingHistogram =
    new StreamingHistogram(size, minimumSeen, maximumSeen)

  def apply(other: Histogram[Double]): StreamingHistogram = {
    val h = apply(other.maxBucketCount)
    other.foreach(h.countItem _)
    h
  }

  def fromTile(r: Tile): StreamingHistogram =
    fromTile(r, DEFAULT_NUM_BUCKETS)

  def fromTile(r: Tile, numBuckets: Int): StreamingHistogram = {
    val h = StreamingHistogram(numBuckets)
    r.foreachDouble  { z => if (isData(z)) h.countItem(z) }
    h
  }
}

/**
  * Ben-Haim, Yael, and Elad Tom-Tov. "A streaming parallel decision
  * tree algorithm."  The Journal of Machine Learning Research 11
  * (2010): 849-872.
  */
class StreamingHistogram(
  size: Int,
  minimum: Double = Double.PositiveInfinity,
  maximum: Double = Double.NegativeInfinity
) extends MutableHistogram[Double] {

  private var _min = minimum
  private var _max = maximum
  private val _buckets = new TreeMap[Double, Long]
  private val _deltas = new TreeMap[Delta, Unit](new DeltaCompare)

  /**
    * Compute the area of the curve between the two buckets.
    */
  private def computeArea(a: Bucket, b: Bucket): Double = {
    val Bucket(value1, count1) = a
    val Bucket(value2, count2) = b
    val small = if (count1 >= 0 && count2 >= 0) min(count1, count2); else max(count1, count2)
    val big = if (count1 >= 0 && count2 >= 0) max(count1, count2); else min(count1, count2)
    val width = abs(value1 - value2)
    (width * small) + (0.5 * width * (big - small))
  }

  /**
    * Take two buckets and return their composite.
    */
  private def compose(left: Bucket, right: Bucket): Bucket = {
    val Bucket(value1, count1) = left
    val Bucket(value2, count2) = right

    if (count1 + count2 != 0)
      Bucket((value1*count1 + value2*count2) / (count1 + count2), (count1 + count2))
    else {
      if (count1 == 0) left
      else if (count2 == 0) right
      else Bucket(0.0, 0)
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
    val Delta(_, middle1, middle2) = delta
    val middle = compose(middle1, middle2)
    val left = {
      val entry = _buckets.lowerEntry(middle1.label)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }
    val right = {
      val entry = _buckets.higherEntry(middle2.label)
      if (entry != null) Some(entry.getKey, entry.getValue); else None
    }

    /* remove middle1 and middle2, as well as the delta between them.*/
    _buckets.remove(middle1.label)
    _buckets.remove(middle2.label)
    _deltas.remove(delta)

    /* Remove delta to the left of the combined buckets */
    if (left != None) {
      val oldDelta = middle1._1 - left.get._1
      _deltas.remove(Delta(oldDelta, left.get, middle1))
    }

    /* Remove delta to the right of the combined buckets */
    if (right != None) {
      val oldDelta = right.get._1 - middle2._1
      _deltas.remove(Delta(oldDelta, middle2, right.get))
    }

    /* Add delta covering the whole range */
    if (left != None && right != None) {
      val delta = right.get._1 - left.get._1
      _deltas.put(Delta(delta, left.get, right.get), Unit)
    }

    /* Add the average of the two combined buckets */
    countItem(middle)
  }

  /**
    * Add a bucket to this histogram.  This can be used to add a new
    * sample to the histogram by letting the bucket-count be equal to
    * one, or it can be used to incrementally merge two histograms.
    */
  private def countItem(b: Bucket): Unit = {
    if (b.label < _min && b.count != 0L) _min = b.label
    if (b.label > _max && b.count != 0L) _max = b.label

    /* First entry */
    if (_buckets.size == 0)
      _buckets.put(b.label, b.count)
    /* Duplicate entry */
    else if (_buckets.containsKey(b.label)) {
      _buckets.put(b.label, _buckets.get(b.label) + b.count)
      return
    }
    /* Create new entry */
    else {
      val smaller = {
        val entry = _buckets.lowerEntry(b.label)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }
      val larger = {
        val entry = _buckets.higherEntry(b.label)
        if (entry != null) Some(entry.getKey, entry.getValue); else None
      }

      /* Remove delta containing new bucket */
      if (smaller != None && larger != None) {
        val large = larger.get
        val small = smaller.get
        val delta = large._1 - small._1
        _deltas.remove(Delta(delta, small, large))
      }

      /* Add delta between new bucket and next-largest bucket */
      if (larger != None) {
        val large = larger.get
        val delta = large._1 - b._1
        _deltas.put(Delta(delta, b, large), Unit)
      }

      /* Add delta between new bucket and next-smallest bucket */
      if (smaller != None) {
        val small = smaller.get
        val delta = b._1 - small._1
        _deltas.put(Delta(delta, small, b), Unit)
      }
    }

    _buckets.put(b.label, b.count)
    if (_buckets.size > size) combine()
  }

  /**
    * Note the occurance of 'item'.
    *
    * The optional parameter 'count' allows histograms to be built
    * more efficiently. Negative counts can be used to remove a
    * particular number of occurances of 'item'.
    */
  def countItem(item: Double, count: Long): Unit =
    countItem((item, count))


  /**
    * Note the occurance of 'item'.
    *
    * The optional parameter 'count' allows histograms to be built
    * more efficiently. Negative counts can be used to remove a
    * particular number of occurances of 'item'.
    */
  def countItemInt(item: Int, count: Long): Unit =
    countItem((item.toDouble, count))

  /**
    * Note the occurances of 'items'.
    */
  def countItems(items: Seq[Bucket]): Unit =
    items.foreach({ item => countItem(item) })

  /**
    * Note the occurances of 'items'.
    */
  def countItems(items: Seq[Double])(implicit dummy: DummyImplicit): Unit =
    items.foreach({ item => countItem((item, 1L)) })

  /**
    * Uncount item.
    *
    * @note _min and _max, the minimum and maximum values seen by the histogram,  are not changed by this.
    */
  def uncountItem(item: Double): Unit =
    countItem((item, -1L))

  /**
    * Get the (approximate) number of occurrences of an item.
    */
  def itemCount(item: Double): Long = {
    if (bucketCount() == 0) 0
    else if (bucketCount() == 1) {
      val Bucket(item2, count) = buckets().head
      count / exp(abs(7 * (item2 - item))).toInt
    }
    else if (_buckets.containsKey(item)) {
      _buckets.get(item)
    }
    else {
      val lo = _buckets.lowerEntry(item)
      val hi = _buckets.higherEntry(item)

      if (lo == null) hi.getValue
      else if (hi == null) lo.getValue
      else {
        val x = ((item-lo.getKey) / (hi.getKey-lo.getKey))
        val result = ((1.0-x)*lo.getValue) + (x*hi.getValue)
        result.toLong
      }
    }
  }

  /**
    * Make a change to the distribution to approximate changing the
    * value of a particular item.
    *
    * @note _min and _max, the minimum and maximum values seen by the histogram,  are not changed by this.
    */
  def setItem(item: Double, count: Long): Unit = {
    val oldCount = itemCount(item)
    countItem(item, -oldCount)
    countItem(item, count)
  }

  /**
    * Return an array of bucket values.
    */
  def values(): Array[Double] = buckets.map(_.label).toArray
  def rawValues(): Array[Double] = values()

  /**
    * Execute the given function on each bucket.  The value contained
    * by the bucket is a Double, and the count is an integer (ergo the
    * signature of the function 'f').
    */
  def foreach(f: (Double, Long) => Unit): Unit =
    buckets.map({ case Bucket(item, count) => f(item, count) })

  /**
    * Execute the given function on each bucket label.
    */
  def foreachValue(f: Double => Unit): Unit =
    buckets.map({ case Bucket(item, _) => f(item) })

  /**
    * Generate Statistics.
    */
  def statistics(): Option[Statistics[Double]] = {
    val zmin = minValue
    val zmax = maxValue

    if (zmin.nonEmpty && zmax.nonEmpty) {
      val dataCount = totalCount
      val localMean = mean.get
      val localMedian = median.get
      val localMode = mode.get
      val ex2 = buckets.map({ case Bucket(item, count) => item * item * count }).sum / totalCount
      val stddev = sqrt(ex2 - localMean * localMean)

      Some(Statistics[Double](dataCount, localMean, localMedian, localMode, stddev, zmin.get, zmax.get))
    }
    else
      None
  }

  /**
    * Update this histogram with the entries from another.
    */
  def update(other: Histogram[Double]): Unit =
    other.foreach({ case (item, count) => this.countItem((item, count)) })

  /**
   * Return a mutable copy of this histogram.
   */
  def mutable(): StreamingHistogram = {
    val sh = StreamingHistogram(this.size, this._min, this._max)
    sh.countItems(this.buckets)
    sh
  }

  /**
    * Create a new histogram from this one and another without
    * altering either.
    */
  def +(other: StreamingHistogram): StreamingHistogram = {
    val sh = StreamingHistogram(this.size, this._min, this._max)
    sh.countItems(this.buckets)
    sh.countItems(other.buckets)
    sh
  }

  /**
    * The number of buckets utilized by this [[Histogram]].
    */
  def bucketCount():Int = _buckets.size

  /**
    * Return the maximum number of buckets of this histogram.
    */
  def maxBucketCount(): Int = size

  /**
    * Return the sum of this histogram and the given one (the sum is
    * the histogram that would result from seeing all of the values
    * seen by the two antecedent histograms).
    */
  def merge(histogram: Histogram[Double]): StreamingHistogram = {
    val sh = StreamingHistogram(this.size, this._min, this._max)
    sh.countItems(this.buckets)
    histogram.foreach({ (item: Double, count: Long) => sh.countItem((item, count)) })
    sh
  }

  /**
    * Return the approximate mode of the distribution.  This is done
    * by simply returning the label of most populous bucket (so this
    * answer could be really bad).
    */
  def mode(): Option[Double] = {
    if (totalCount <= 0)
      None
    else
      Some(buckets.reduce({ (l,r) => if (l._2 > r._2) l; else r })._1)
  }

  /**
    * Return the approximate median of the histogram.
    */
  def median(): Option[Double] = {
    if (totalCount <= 0)
      None
    else
      Some(percentile(0.50))
  }

  /**
    *  Return the approximate mean of the histogram.
    */
  def mean(): Option[Double] = {
    if (totalCount <= 0)
      None
    else {
      val weightedSum =
        buckets.foldLeft(0.0)({ (acc,bucket) => acc + (bucket.label * bucket.count) })
      Some(weightedSum / totalCount)
    }
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
  def totalCount(): Long = buckets.map(_._2).sum

  /**
    * Get the minimum value this histogram has seen.
    */
  def minValue(): Option[Double] =
    if(_min ==  Double.PositiveInfinity) {
      None
    } else {
      Some(_min)
    }

  /**
    * Gets the maximum value this histogram has seen
    */
  def maxValue(): Option[Double] =
    if(_max == Double.NegativeInfinity) {
      None
    } else {
      Some(_max)
    }

  /**
    * Return an array of x, cdf(x) pairs
    */
  def cdf(): Array[(Double, Double)] = {
    val bs = buckets
    val labels = bs.map(_.label)
    val pdf = bs.map(_.count.toDouble / totalCount)

    labels.zip(pdf.scanLeft(0.0)(_ + _).drop(1)).toArray
  }

  /**
    * This returns a tuple of tuples, where the inner tuples contain a
    * bucket label and its percentile.
    */
  private def cdfIntervals(): Iterator[((Double, Double), (Double, Double))] = {
    if(buckets.size < 2) {
      Iterator.empty
    } else {
      val bs = buckets
      val n = totalCount
      // We have to prepend the minimum value here
      val ds = minValue().getOrElse(Double.NegativeInfinity) +: bs.map(_.label)
      val pdf = bs.map(_.count.toDouble / n)
      val cdf = pdf.scanLeft(0.0)(_ + _)
      val data = ds.zip(cdf).sliding(2)

      data.map({ ab => (ab.head, ab.tail.head) })
    }
  }

  /**
    * Get the (approximate) percentile of this item.
    */
  def percentileRanking(item: Double): Double =
    if(buckets.size == 1) {
      if(item < buckets.head.label) 0.0 else 1.0
    } else {
      val data = cdfIntervals
      val tt = data.dropWhile(_._2._1 <= item).next
      val (d1, pct1) = tt._1
      val (d2, pct2) = tt._2
      if(item - d1 < 0.0) {
        0.0
      } else {
        val x = (item - d1) / (d2 - d1)

        (1-x)*pct1 + x*pct2
      }
    }

  /** For each q in qs, all between 0 and 1, find a number (approximately) at the qth percentile.
    *
    * @param qs   A list of quantiles (0.01 == 1th pctile, 0.2 == 20th pctile) to use in
    *              generating breaks
    *
    * @note       Our aim here is to produce values corresponding to the qs which stretch
    *              from minValue to maxValue, interpolating based on observed bins along the way
    */
  def percentileBreaks(qs: Seq[Double]): Seq[Double] = {
    if(buckets.size == 1) {
      qs.map(z => buckets.head.label)
    } else {
      val data = cdfIntervals
      if(!data.hasNext) {
        Seq()
      } else {
        val result = MutableListBuffer[Double]()
        var curr = data.next

        def getValue(q: Double): Double = {
          val (d1, pct1) = curr._1
          val (d2, pct2) = curr._2
          val proportionalDiff = (q - pct1) / (pct2 - pct1)
          (1 - proportionalDiff) * d1 + proportionalDiff * d2
        }

        val quantilesToCheck =
          if (qs.head < curr._2._2) {
            // The first case. Either the first bin IS the minimum
            // value or it is VERY close (because it is the result of
            // combining the minValue bin with neighboring bins)
            result += curr._1._1

            // IF the minvalue is the same as the lowest bin, we need
            // to clean house and remove the lowest bin.  Else, we
            // have to treat the lowest bin as the 0th pctile for
            // interpolation.
            if (curr._1._1 == curr._2._1) { curr = (curr._1, data.next._2) }
            else { curr = ((curr._1._1, 0.0), curr._2) }
            qs.tail
          } else {
            qs
          }

        for(q <- quantilesToCheck) {
          // Catch the edge case of 0th pctile, which usually won't matter
          if (q == 0.0) { result += minValue().getOrElse(Double.NegativeInfinity) }
          else if (q == 1.0) { result += maxValue().getOrElse(Double.PositiveInfinity) }
          else {
            if(q < curr._2._2) {
              result += getValue(q)
            } else {
              while(data.hasNext && curr._2._2 <= q) { curr = data.next }
              result += getValue(q)
            }
          }
        }

        result.toSeq
      }
    }
  }

  def percentile(q: Double): Double =
    percentileBreaks(List(q)).head

  /**
    * This method returns the (approximate) quantile breaks of the
    * distribution of points that the histogram has seen so far.  It
    * is guaranteed that no value in the returned array will be
    * outside the minimum-maximum range of values seen.
    *
    * @param  num  The number of breaks desired
    */
  def quantileBreaks(num: Int): Array[Double] =
    percentileBreaks((1 to num).map(_ / num.toDouble)).toArray

  /**
    * Return the list of buckets of this histogram.  Primarily useful
    * for debugging and serialization.
    */
  def buckets(): List[Bucket] = _buckets.asScala.map { tup => tup: Bucket }.toList

  /**
    * Return the list of deltas of this histogram.  Primarily useful
    * for debugging.
    */
  def deltas(): List[Delta] = _deltas.keySet.asScala.toList
}
