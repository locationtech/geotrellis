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
class ModeSpec extends FunSpec with FocalOpSpec
                               with ShouldMatchers {

  val getModeResult = Function.uncurried((getCursorResult _).curried((r,n) => Mode(r,n)))

  describe("Mode") {
    it("should match histogram mode default set") {      
      for(s <- defaultTestSets) {
        val h = FastMapHistogram()
        for(x <- s.filter { v => v != NODATA}) { h.countItem(x,1) }
        getModeResult(Square(1),MockCursor.fromAll(s:_*)) should equal (h.getMode)
      }
    }
  }
}
