package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._
import geotrellis.raster._

import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.matchers._

import org.scalatest.junit.JUnitRunner

// 0  1  2  3
// 4  5  6  7
// 8  9 10 11
//12 13 14 15

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
        getMinResult(Square(1),MockCursor.fromAll(s:_*)) should equal ({
          val x = s.filter(isData(_))
          if(x.isEmpty) NODATA else x.min
        })
      }
    }

    it("should square min for raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,1,      1,3,5,      9,8,2,
                9,1,1,      2,2,2,      4,3,5,

                3,8,1,      3,3,3,      1,2,2,
                2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      getSource(rs1.focalMin(Square(1))) match {
        case Complete(result,success) =>
//          println(success)
          assertEqual(result,
            Array(1, 1, 1,    1, 1, 2,    2, 2, 2,
                  1, 1, 1,    1, 1, 1,    1, 1, 2,

                  1, 1, 1,    1, 1, 1,    1, 1, 2,
                  2, 1, 1,    1, 1, 1,    1, 1, 2))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should square min with 5x5 neighborhood") {
      val rs1 = createRasterSource(
        Array( nd,7,7,      7,3,5,      9,8,2,
                9,7,7,      2,2,2,      4,3,5,

                3,8,7,      3,3,3,      7,4,5,
                9,4,7,     7,nd,7,      8,4,3
        ),
        3,2,3,2
      )

      getSource(rs1.focalMin(Square(2))) match {
        case Complete(result,success) =>
//          println(success)
          assertEqual(result,
            Array(3, 2, 2,    2, 2, 2,    2, 2, 2,
                  3, 2, 2,    2, 2, 2,    2, 2, 2,

                  3, 2, 2,    2, 2, 2,    2, 2, 2,
                  3, 2, 2,    2, 2, 2,    2, 2, 3))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should circle min for raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,4,     5, 4,2,      9,nd,nd,
                9,6,2,     2, 2,2,      5,3,nd,

                3,8,4,     3, 3,3,      3,9,2,
                2,9,7,     4,nd,9,      8,8,4
        ),
        3,2,3,2
      )

      getSource(rs1.focalMin(Circle(1))) match {
        case Complete(result,success) =>
          //println(success)
          assertEqual(result,
            Array(7, 4, 2,    2, 2, 2,    2, 3, nd,
                  3, 2, 2,    2, 2, 2,    2, 3, 2,

                  2, 3, 2,    2, 2, 2,    3, 2, 2,
                  2, 2, 4,    3, 3, 3,    3, 4, 2))
        case Error(msg,failure) =>
          // println(msg)
          // println(failure)
          assert(false)

      }
    }
  }
}
