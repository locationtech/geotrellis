package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MinTest extends FunSuite {
  val e = Extent(0.0, 0.0, 4.0, 4.0)
  val re = RasterExtent(e, 1.0, 1.0, 4, 4)

  val server = geotrellis.process.TestServer()

  val r = Raster(IntArrayRasterData((0 until 16).toArray, 4, 4), re)

  def runx(op:Operation[Raster]) = server.run(op).toArray

  test("square min r=1") {
    assert(runx(Min(r, Square(1))) === Array(0, 0, 1, 2,
                                             0, 0, 1, 2,
                                             4, 4, 5, 6,
                                             8, 8, 9, 10))
  }

  test("square min r=2") {
    assert(runx(Min(r, Square(2))) === Array(0, 0, 0, 1,
                                             0, 0, 0, 1,
                                             0, 0, 0, 1,
                                             4, 4, 4, 5))
  }

  test("square min r=3+") {
    val data0 = (0 until 16).map(z => 0).toArray
    assert(runx(Min(r, Square(3))) === data0)
    assert(runx(Min(r, Square(4))) === data0)
    assert(runx(Min(r, Square(5))) === data0)
  }

  test("circle min r=1") {
    assert(runx(Min(r, Square(1))) === Array(0, 0, 1, 2,
                                             0, 0, 1, 2,
                                             4, 4, 5, 6,
                                             8, 8, 9, 10))
  }

  test("circle min r=2") {
    assert(runx(Min(r, Circle(2))) === Array(0, 0, 0, 1,
                                             0, 0, 1, 2,
                                             0, 1, 2, 3,
                                             4, 5, 6, 7))
  }

  test("circle min r=3") {
    val mask = new CursorMask(7,Circle(3).mask)
    println(mask.asciiDraw)
    assert(runx(Min(r, Circle(3))) === Array(0, 0, 0, 0,
                                             0, 0, 0, 1,
                                             0, 0, 0, 1,
                                             0, 1, 2, 3))
  }

  test("circle min r=4+") {
    val data0 = (0 until 16).map(z => 0).toArray
    assert(runx(Min(r, Circle(4))) === Array(0, 0, 0, 0,
                                             0, 0, 0, 0,
                                             0, 0, 0, 0,
                                             0, 0, 0, 1))
    assert(runx(Min(r, Circle(5))) === data0)
    assert(runx(Min(r, Circle(6))) === data0)
  }
}
