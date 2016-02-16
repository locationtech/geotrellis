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
    it("should return NODATA if no items are counted") {
      val h = StreamingHistogram()
      h.getMode.isNaN should equal (true)
    }

    it("should return the same result for getMode and generateStatistics.mode") {
      val h = StreamingHistogram()

      list3.foreach({i => h.countItem(i) })

      val mode = h.getMode()
      mode should equal (59049)
      mode should equal (h.generateStatistics.mode)
    }

    it(".getMode and .generateStatistics.mode should agree on a mode of a unique list") {
      val h = StreamingHistogram()
      val list = List(9, 8, 7, 6, 5, 4, 3, 2, -10)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.getMode()
      mode should equal (h.generateStatistics.mode)
    }
  }

  describe("median calculations") {
    it("should return the same result for getMedian and generateStatistics.median") {
      val h = StreamingHistogram()

      list1.foreach({ i => h.countItem(i) })

      h.getMedian should equal (8.75)
      h.getMedian should equal (h.generateStatistics.median)
    }

    it("getMedian should work when n is large with repeated elements") {
      val h = StreamingHistogram()

      Iterator.continually(list1)
        .flatten.take(list1.length * 10000)
        .foreach({ i => h.countItem(i) })

      h.getMedian should equal (8.75)
      h.getMedian should equal (h.generateStatistics.median)
    }

    it("getMedian should work when n is large with unique elements") {
      val h = StreamingHistogram()

      Iterator.continually(list1)
        .flatten.take(list1.length * 10000)
        .foreach({ i => h.countItem(i + (3.0 + r.nextGaussian) / 60000.0) })

      h.getMedian.toInt should equal (9)
      h.getMedian should equal (h.generateStatistics.median)
    }
  }

  describe("mean calculation") {
    it("should return the same result for getMean and generateStatistics.mean") {
      val h = StreamingHistogram()

      list2.foreach({ i => h.countItem(i) })

      val mean = h.getMean()
      abs(mean - 18194.14285714286) should be < 1e-7
      mean should equal (h.generateStatistics.mean)
    }

    it("getMean should work when n is large with repeated elements") {
      val h = StreamingHistogram()

      Iterator.continually(list2)
        .flatten.take(list2.length * 10000)
        .foreach({ i => h.countItem(i) })

      val mean = h.getMean()
      abs(mean - 18194.14285714286) should be < 1e-7
      mean should equal (h.generateStatistics.mean)
    }

    it("getMean should work when n is large with unique elements") {
      val h = StreamingHistogram()

      Iterator.continually(list2)
        .flatten.take(list2.length * 10000)
        .foreach({ i => h.countItem(i + r.nextGaussian / 10000.0) })

      val mean = h.getMean()
      abs(mean - 18194.14285714286) should be < 1e-4
      mean should equal (h.generateStatistics.mean)
    }
  }

}
