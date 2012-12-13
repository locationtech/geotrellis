package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster.op._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._
import org.scalatest.junit.JUnitRunner

import scala.math._

@RunWith(classOf[JUnitRunner])
class MaxSpec extends FunSpec with FocalOpSpec
                              with ShouldMatchers 
                              with TestServer {

  val getMaxResult = Function.uncurried((getCursorResult _).curried((r,n) => focal.Max(r,n)))
  val getMaxSetup = Function.uncurried((getSetup _).curried((r,n) => focal.Max(r,n)))
  val squareSetup = getMaxSetup(defaultRaster,Square(1))

  describe("Max") {
    it("should correctly compute a center neighborhood") {
      squareSetup.getResult(2,2) should equal (4)
    }

    it("should agree with a manually worked out example") {
      val r = createRaster(Array[Int](1,1,1,1,
                                      2,2,2,2,
                                      3,3,3,3,
                                      1,1,4,4))

      val maxOp = focal.Max(r,Square(1))
      assertEqual(maxOp, Array[Int](2,2,2,2,
                                    3,3,3,3,
                                    3,4,4,4,
                                    3,4,4,4))
    }

    it("should match scala.math.max default sets") {      
      for(s <- defaultTestSets) {
        getMaxResult(Square(1),MockCursor.fromAll(s:_*)) should equal (s.max)
      }
    }
  }
}
