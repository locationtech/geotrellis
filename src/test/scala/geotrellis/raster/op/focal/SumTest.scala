package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SumTest extends FunSuite {
  val sq1 = Square(1)
  val sq2 = Square(2)
  val sq3 = Square(3)

  val e = Extent(0.0, 0.0, 4.0, 4.0)
  val re = RasterExtent(e, 1.0, 1.0, 4, 4)

  val server = geotrellis.process.TestServer()

  val r = Raster(IntConstant(1, 4, 4), re)

  val data16 = Array(16, 16, 16, 16,
                     16, 16, 16, 16,
                     16, 16, 16, 16,
                     16, 16, 16, 16)

  def runx(op:Operation[Raster]) = server.run(op).toArray

  test("square sum r=1") {
    assert(runx(Sum(r, Square(1))) === Array(4, 6, 6, 4,
                                             6, 9, 9, 6,
                                             6, 9, 9, 6,
                                             4, 6, 6, 4))
  }

  test("square sum r=2") {
    assert(runx(Sum(r, Square(2))) === Array(9, 12, 12, 9,
                                             12, 16, 16, 12,
                                             12, 16, 16, 12,
                                             9, 12, 12, 9))
  }

  test("square sum r=3+") {
    assert(runx(Sum(r, Square(3))) === data16)
    assert(runx(Sum(r, Square(4))) === data16)
    assert(runx(Sum(r, Square(5))) === data16)
  }

  test("circle sum r=1") {
    assert(runx(Sum(r, Circle(1))) === Array(3, 4, 4, 3,
                                             4, 5, 5, 4,
                                             4, 5, 5, 4,
                                             3, 4, 4, 3))
  }

  test("circle sum r=2") {
    assert(runx(Sum(r, Circle(2))) === Array(6, 8, 8, 6,
                                             8, 11, 11, 8,
                                             8, 11, 11, 8,
                                             6, 8, 8, 6))
  }

  test("circle sum r=3") {
    assert(runx(Sum(r, Circle(3))) === Array(11, 13, 13, 11,
                                             13, 16, 16, 13,
                                             13, 16, 16, 13,
                                             11, 13, 13, 11))
  }

  test("circle sum r=4+") {
    assert(runx(Sum(r, Circle(4))) === Array(15, 16, 16, 15,
                                             16, 16, 16, 16,
                                             16, 16, 16, 16,
                                             15, 16, 16, 15))
    assert(runx(Sum(r, Circle(5))) === data16)
    assert(runx(Sum(r, Circle(6))) === data16)
  }
}
