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
}
                               
