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

package geotrellis.statistics

import geotrellis._

import org.scalatest._

class FastMapHistogramSpec extends FunSpec with Matchers {
  describe("getMode") {
    it("should return NODATA if no items are counted") {
      val h = FastMapHistogram()
      h.getMode should equal (NODATA)
    }
  }

  describe("median calculations") {
    it("should return the same result for getMedian and generateStatistics.median") {
      val h = FastMapHistogram()

      for(i <- List(1,2,3,3,4,5,6,6,6,7,8,9,9,9,9,10,11,12,12,13,14,14,15,16,17,17,18,19)) {
        h.countItem(i)
      }

      h.getMedian should equal (9)
      h.getMedian should equal (h.generateStatistics.median)
    }
  }

  describe("mean calculation") {
    it("should return the same result for getMean and generateStatistics.mean") {
      val h = FastMapHistogram()
      //large values to check that overflow does not trip as it would with sum/count method
      val list = List(1, 32, 243, 243, 1024, 3125, 7776, 7776, 7776, 16807, 32768, 59049, 59049,
        59049, 59049, 100000, 161051, 248832, 248832, 371293, 537824, 537824, 759375, 1048576,
        1419857, 1419857, 1889568, 2476099, 2147483647)
      for(i <- list) {
        h.countItem(i)
      }

      val mean = h.getMean()
      mean should equal (7.444884144827585E7)
      mean should equal (h.generateStatistics.mean)
    }
  }

  describe("mode calculation") {
    it("should return the same result for getMode and generateStatistics.mode") {
      val h = FastMapHistogram()
      val list = List(1, 32, 243, 243, 1024, 1024, 7776, 7776, 7776, 16807, 32768, 59049, 59049,
        59049, 59049, 100000, 161051, 248832, 248832, 371293, 537824, 537824, 759375, 1048576,
        1419857, 1419857, 1889568, 2476099, 2147483647)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.getMode()
      mode should equal (59049)
      mode should equal (h.generateStatistics.mode)
    }

    it(".getMode and .generateStatistics.mode should agree on a mode of a unique list") {
      val h = FastMapHistogram()
      val list = List(9, 8, 7, 6, 5, 4, 3, 2, -10)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.getMode()
      mode should equal (h.generateStatistics.mode)
    }
  }

}
                               
