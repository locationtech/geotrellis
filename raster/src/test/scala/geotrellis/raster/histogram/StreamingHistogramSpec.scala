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
import geotrellis.raster.io._
import spray.json._

import org.scalatest._
import math.abs
import scala.util.Random

class StreamingHistogramSpec extends FunSpec with Matchers {
  val r = Random
  val list1 = List(1,2,3,3,4,5,6,6,6,7,8,9,9,9,9,10,11,12,12,13,14,14,15,16,17,17,18,19)
  val list2 = List(1, 32, 243, 243, 1024, 3125, 7776, 7776, 7776, 16807, 32768, 59049, 59049, 59049)
  val list3 = List(1, 32, 243, 243, 1024, 1024, 7776, 7776, 7776, 16807, 32768, 59049, 59049,
    59049, 59049, 100000, 161051, 248832, 248832, 371293, 537824, 537824, 759375, 1048576,
    1419857, 1419857, 1889568, 2476099, 2147483647)

  describe("mode calculation") {
    it("should return None if no items are counted") {
      val h = StreamingHistogram()
      h.mode should be (None)
    }

    it("should return the same result for mode and statistics.mode") {
      val h = StreamingHistogram()

      list3.foreach({i => h.countItem(i) })

      val mode = h.mode.get
      mode should equal (59049)
      mode should equal (h.statistics.get.mode)
    }

    it(".mode and .statistics.mode should agree on a mode of a unique list") {
      val h = StreamingHistogram()
      val list = List(9, 8, 7, 6, 5, 4, 3, 2, -10)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.mode.get
      mode should equal (h.statistics.get.mode)
    }
  }

  describe("median calculations") {
    it("should return the same result for median and statistics.median") {
      val h = StreamingHistogram()

      list1.foreach({ i => h.countItem(i) })

      h.median.get should equal (8.75)
      h.median.get should equal (h.statistics.get.median)
    }

    it("median should work when n is large with repeated elements") {
      val h = StreamingHistogram()

      Iterator.continually(list1)
        .flatten.take(list1.length * 10000)
        .foreach({ i => h.countItem(i) })

      h.median.get should equal (8.75)
      h.median.get should equal (h.statistics.get.median)
    }

    it("median should work when n is large with unique elements") {
      val h = StreamingHistogram()

      /* Here  the list of values  is used repeatedly, but  with small
       * perturbations to  make the values  unique (to make  sure that
       * the maximum number of buckets  is exceeded so that the median
       * can be tested under  those circumstances).  The perturbations
       * should be  positive numbers  with magnitude somewhere  in the
       * neighborhood of 1e-4.*/
      Iterator.continually(list1)
        .flatten.take(list1.length * 10000)
        .foreach({ i => h.countItem(i + (3.0 + r.nextGaussian) / 60000.0) })

      math.round(h.median.get).toInt should equal (9)
      h.median.get should equal (h.statistics.get.median)
    }
  }

  describe("mean calculation") {
    it("should return the same result for mean and statistics.mean") {
      val h = StreamingHistogram()

      list2.foreach({ i => h.countItem(i) })

      val mean = h.mean.get
      abs(mean - 18194.14285714286) should be < 1e-7
      mean should equal (h.statistics.get.mean)
    }

    it("mean should work when n is large with repeated elements") {
      val h = StreamingHistogram()

      Iterator.continually(list2)
        .flatten.take(list2.length * 10000)
        .foreach({ i => h.countItem(i) })

      val mean = h.mean.get
      abs(mean - 18194.14285714286) should be < 1e-7
      mean should equal (h.statistics.get.mean)
    }

    it("mean should work when n is large with unique elements") {
      val h = StreamingHistogram()

      /* The  list of values is  used repeatedly here, but  with small
       * perturbations.   The motivation  for  those  is similar  that
       * stated above for the median case.  The difference of the mean
       * of the perturbed list and the mean of the unperturbed list is
       * a random variable with mean zero and standard deviation 1e-6,
       * so this test "should never fail" unless the histogram code is
       * faulty. */
      Iterator.continually(list2)
        .flatten.take(list2.length * 10000)
        .foreach({ i => h.countItem(i + r.nextGaussian / 10000.0) })

      val mean = h.mean.get
      abs(mean - 18194.14285714286) should be < 1e-4
      mean should equal (h.statistics.get.mean)
    }
  }

  describe("quantileBreaks") {
    it("should return a single element when only one type of value has been counted") {
      val arrTile = FloatArrayTile.fill(1.0f, 100, 200, FloatConstantNoDataCellType)
      val hist = arrTile.histogramDouble
      hist.quantileBreaks(5) should be (Seq(1.0, 1.0, 1.0, 1.0, 1.0))
    }

    it("should return a single element when only one type of value has been counted, merged with an empty tile histogram") {
      val arrTile = FloatArrayTile.fill(1.0f, 100, 200, FloatConstantNoDataCellType)
      val arrTile2 = FloatArrayTile.empty(100, 200, FloatConstantNoDataCellType)
      val hist = arrTile.histogramDouble.merge(arrTile2.histogramDouble)
      hist.quantileBreaks(5) should be (Seq(1.0, 1.0, 1.0, 1.0, 1.0))
    }

    it("should get quantile breaks on tile with only 2 types of values, interpolating between") {
      val arrTile = FloatArrayTile.empty(100, 200)
      arrTile.setDouble(5, 3, 1.0)
      arrTile.setDouble(5, 5, 2.0)
      val hist = arrTile.histogramDouble
      val breaks = hist.quantileBreaks(100)
      breaks.head should be (1.0)
      breaks.last should be (2.0)
      breaks.tail.init.map { _ should (be > 1.0 and be < 2.0) }
    }

    it("should not throw when there are more breaks than buckets") {
      val h = StreamingHistogram()

      Iterator
        .continually(List(1,2,3))
        .flatten
        .take(10000)
        .foreach({ i => h.countItem(i.toDouble) })

      val breaks = h.quantileBreaks(50).toList

      breaks.length should be (50)
      breaks.max should be > (2.9)
      breaks.min should be < (1.1)
    }
  }

  describe("Json Serialization") {
    it("should successfully round-trip a trivial histogram") {
      val h1 = StreamingHistogram()
      val h2 = (h1: Histogram[Double]).toJson.prettyPrint.parseJson.convertTo[Histogram[Double]]

      h1.statistics should equal (h2.statistics)
      h1.quantileBreaks(42) should equal (h2.quantileBreaks(42))
      h1.bucketCount should equal (h2.bucketCount)
      h1.maxBucketCount should equal (h2.maxBucketCount)
    }

    it("should successfully round-trip a non-trivial histogram") {
      val h1 = StreamingHistogram()

      Iterator
        .continually(list1)
        .flatten
        .take(10000)
        .foreach({ i => h1.countItem(i.toDouble) })

      val h2 = (h1: Histogram[Double]).toJson.prettyPrint.parseJson.convertTo[Histogram[Double]]

      h1.statistics should equal (h2.statistics)
      h1.quantileBreaks(42) should equal (h2.quantileBreaks(42))
      h1.bucketCount should equal (h2.bucketCount)
      h1.maxBucketCount should equal (h2.maxBucketCount)
    }

    it("should produce a result which behaves the same as the original") {
      val h1 = StreamingHistogram()

      Iterator
        .continually(list1)
        .flatten
        .take(10000)
        .foreach({ i => h1.countItem(i.toDouble) })

      val h2 = StreamingHistogram((h1: Histogram[Double]).toJson.prettyPrint.parseJson.convertTo[Histogram[Double]])

      Iterator
        .continually(list2)
        .flatten
        .take(20000)
        .foreach({ i =>
          h1.countItem(i.toDouble)
          h2.countItem(i.toDouble)
        })

      h1.statistics should equal (h2.statistics)
      h1.quantileBreaks(42) should equal (h2.quantileBreaks(42))
      h1.bucketCount should equal (h2.bucketCount)
      h1.maxBucketCount should equal (h2.maxBucketCount)
    }

    it("should produce non-sterile offspring") {
      val h1 = StreamingHistogram()

      Iterator
        .continually(list1)
        .flatten
        .take(10000)
        .foreach({ i => h1.countItem(i.toDouble) })

      val h2 = {
        var h: Histogram[Double] = h1
        var i = 0; while (i < 107) {
          h = (h: Histogram[Double]).toJson.prettyPrint.parseJson.convertTo[Histogram[Double]]
          i += 1
        }
        StreamingHistogram(h)
      }

      Iterator
        .continually(list2)
        .flatten
        .take(20000)
        .foreach({ i =>
          h1.countItem(i.toDouble)
          h2.countItem(i.toDouble)
        })

      h1.statistics should equal (h2.statistics)
      h1.quantileBreaks(42) should equal (h2.quantileBreaks(42))
      h1.bucketCount should equal (h2.bucketCount)
      h1.maxBucketCount should equal (h2.maxBucketCount)
    }

  }

  it("should allow the bucket count to be parameterized") {
    val tile = IntArrayTile(1 to 250*250 toArray, 250, 250)

    val default = tile.histogramDouble()
    val custom = tile.histogramDouble(200)

    default.statistics should not be (custom.statistics)
  }

  describe("Counting") {
    it("binCounts should report non-zero bin counts") {
      val tile = DoubleArrayTile(Array[Double](52, 54, 61, 32, 52, 50, 11, 21, 18), 3, 3)
      val result = tile.histogramDouble(3)
      result.binCounts.map({ pair => pair._2 > 0.0 }) should be (Array(true, true, true))
    }

    it("itemCount should report non-zero values when appropriate") {
      val tile = DoubleArrayTile(Array[Double](52, 54, 61, 32, 52, 50, 11, 21, 18), 3, 3)
      val result = tile.histogramDouble(3)
      result.itemCount(16.7) should be > 0L
    }
  }
}
