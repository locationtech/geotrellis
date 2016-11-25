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

import org.scalatest._

class FastMapHistogramSpec extends FunSpec with Matchers {
  describe("mode") {
    it("should return NODATA if no items are counted") {
      val h = FastMapHistogram()
      h.mode should equal (None)
    }
  }

  describe("median calculations") {
    it("should return the same result for median and statistics.median") {
      val h = FastMapHistogram()

      for(i <- List(1,2,3,3,4,5,6,6,6,7,8,9,9,9,9,10,11,12,12,13,14,14,15,16,17,17,18,19)) {
        h.countItem(i)
      }

      h.median.get should equal (9)
      h.median.get should equal (h.statistics.get.median)
    }
  }

  describe("mean calculation") {
    it("should return the same result for mean and statistics.mean") {
      val h = FastMapHistogram()
      //large values to check that overflow does not trip as it would with sum/count method
      val list = List(1, 32, 243, 243, 1024, 3125, 7776, 7776, 7776, 16807, 32768, 59049, 59049,
        59049, 59049, 100000, 161051, 248832, 248832, 371293, 537824, 537824, 759375, 1048576,
        1419857, 1419857, 1889568, 2476099, 2147483647)
      for(i <- list) {
        h.countItem(i)
      }

      val mean = h.mean()
      mean.get should equal (7.444884144827585E7)
      mean.get should equal (h.statistics.get.mean)
    }
  }

  describe("mode calculation") {
    it("should return the same result for mode and statistics.mode") {
      val h = FastMapHistogram()
      val list = List(1, 32, 243, 243, 1024, 1024, 7776, 7776, 7776, 16807, 32768, 59049, 59049,
        59049, 59049, 100000, 161051, 248832, 248832, 371293, 537824, 537824, 759375, 1048576,
        1419857, 1419857, 1889568, 2476099, 2147483647)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.mode()
      mode.get should equal (59049)
      mode.get should equal (h.statistics.get.mode)
    }

    it(".mode and .statistics.mode should agree on a mode of a unique list") {
      val h = FastMapHistogram()
      val list = List(9, 8, 7, 6, 5, 4, 3, 2, -10)
      for(i <- list) {
        h.countItem(i)
      }

      val mode = h.mode()
      mode.get should equal (h.statistics.get.mode)
    }
  }

  describe("adding large counts") {
    it("should provide correct results for counts > Int.MaxValue") {
      val h = FastMapHistogram ()
      val shft : (Long,Long) => Long = (_ << _)
      h.countItem(10, shft(1,40))

      val count = h.itemCount(10)
      count should equal (1099511627776L)
      
    }
  }
}
