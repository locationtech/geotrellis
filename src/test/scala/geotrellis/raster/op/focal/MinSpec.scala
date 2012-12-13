package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MinSpec extends FunSpec with FocalOpSpec
                              with ShouldMatchers
                              with TestServer {
  describe("Min") {
    it("square min r=1") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Square(1)), Array(0, 0, 1, 2,
                                           0, 0, 1, 2,
                                           4, 4, 5, 6,
                                           8, 8, 9, 10))
    }

    it("square min r=2") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Square(2)), Array(0, 0, 0, 1,
                                           0, 0, 0, 1,
                                           0, 0, 0, 1,
                                           4, 4, 4, 5))
    }

    it("square min r=3+") {
      val r = createRaster((0 until 16).toArray)
      val data0 = (0 until 16).map(z => 0).toArray
      assertEqual(Min(r, Square(3)), data0)
      assertEqual(Min(r, Square(4)), data0)
      assertEqual(Min(r, Square(5)), data0)
    }

    it("circle min r=1") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Square(1)), Array(0, 0, 1, 2,
                                           0, 0, 1, 2,
                                           4, 4, 5, 6,
                                           8, 8, 9, 10))
    }

    it("circle min r=2") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Circle(2)), Array(0, 0, 0, 1,
                                           0, 0, 1, 2,
                                           0, 1, 2, 3,
                                           4, 5, 6, 7))
    }

    it("circle min r=3") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Circle(3)), Array(0, 0, 0, 0,
                                           0, 0, 0, 1,
                                           0, 0, 0, 1,
                                           0, 1, 2, 3))
    }

    it("circle min r=4+") {
      val r = createRaster((0 until 16).toArray)
      val data0 = (0 until 16).map(z => 0).toArray
      assertEqual(Min(r, Circle(4)), Array(0, 0, 0, 0,
                                           0, 0, 0, 0,
                                           0, 0, 0, 0,
                                           0, 0, 0, 1))
      assertEqual(Min(r, Circle(5)), data0)
      assertEqual(Min(r, Circle(6)), data0)
    }

    val getMinResult = Function.uncurried((getCursorResult _).curried((r,n) => Min(r,n)))

    it("should match scala.math.max default sets") {      
      for(s <- defaultTestSets) {        
        getMinResult(Square(1),MockCursor.fromAll(s:_*)) should equal (s.min)
      }
    }
  }
}
