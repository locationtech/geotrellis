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
                               with ShouldMatchers 
                               with RasterBuilders
                               with TestServer {

  val getModeResult = Function.uncurried((getCursorResult _).curried((r,n) => Mode(r,n)))

  describe("Mode") {
    it("should match worked out results") {
      val r = createRaster(Array(3, 4, 1, 1, 1,
                                 7, 4, 0, 1, 0,
                                 3, 3, 7, 7, 1,
                                 0, 7, 2, 0, 0,
                                 6, 6, 6, 5, 5))

      var result = run(Mode(r,Square(1)))

      val beIn:Seq[Int]=>Matcher[Int] = seq => Matcher { x => 
        MatchResult(seq.contains(x), s"${x} was not in ${seq}", s"${x} was in ${seq}") 
      }

      result.get(0,0) should equal (4)
      result.get(1,0) should equal (4)
      result.get(2,0) should equal (1)
      result.get(3,0) should equal (1)
      result.get(4,0) should equal (1)
      result.get(0,1) should equal (3)
      result.get(1,1) should beIn (Seq(3,7))
      result.get(2,1) should equal (1)
      result.get(3,1) should equal (1)
      result.get(4,1) should equal (1)
      result.get(0,2) should beIn (Seq(3,7))
      result.get(1,2) should equal (7)
      result.get(2,2) should equal (7)
      result.get(3,2) should equal (0)
      result.get(4,2) should equal (0)
      result.get(0,3) should beIn (Seq(3,6))
      result.get(1,3) should equal (6)
      result.get(2,3) should equal (7)
      result.get(3,3) should equal (7)
      result.get(4,3) should beIn (Seq(0,5))
      result.get(0,4) should equal (6)
      result.get(1,4) should equal (6)
      result.get(2,4) should equal (6)
      result.get(3,4) should beIn (Seq(0,5))
      result.get(4,4) should beIn (Seq(0,5))
    }
  }
}
