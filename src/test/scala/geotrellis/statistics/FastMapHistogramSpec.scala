package geotrellis.statistics

import geotrellis._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FastMapHistogramSpec extends FunSpec with ShouldMatchers {
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
}
                               
