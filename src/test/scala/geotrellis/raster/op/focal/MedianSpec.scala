package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.statistics._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class MedianSpec extends FunSpec with FocalOpSpec
                                 with ShouldMatchers {

  val getMedianResult = Function.uncurried((getCursorResult _).curried((r,n) => Median(r,n)))

  describe("Median") {
    it("should match histogram median default set in cursor calculation") {      
      for(s <- defaultTestSets) {
        val h = FastMapHistogram()
        for(x <- s) { h.countItem(x,1) }
        getMedianResult(Circle(1),MockCursor.fromAll(s:_*)) should equal (h.getMedian)
      }
    }
  }
}
