package geotrellis.raster.op.focal

import geotrellis._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MoranTest extends FunSuite {
  val x = Array(0, 1, 0, 1, 0, 1, 0, 1)
  val y = Array(1, 0, 1, 0, 1, 0, 1, 0)

  val e = Extent(0.0, 0.0, 8.0, 8.0)
  val re = RasterExtent(e, 1.0, 1.0, 8, 8)
  val arr = (0 until 64).map {
    z => if ((z % 16) < 8) z % 2 else (z + 1) % 2
  }.toArray
  val data = IntArrayRasterData(arr, 8, 8)
  val chess = Raster(data, re)

  import geotrellis.process._
  val server = TestServer()

  test("global square moran (chess)") {
    val n = server.run(ScalarMoransI(chess, Nesw(1)))
    assert(n === -1.0)
  }

  test("global diagonal moran (chess)") {
    val n = server.run(ScalarMoransI(chess, Square(1)))
    assert(n === (-2.0 / 30))
  }

  test("raster square moran (chess)") {
    val r = server.run(RasterMoransI(chess, Nesw(1)))
    assert(r.toArrayDouble === Array.fill(64)(-1.0))
  }
  
  test("raster diagonal moran (chess)") {
    val r = server.run(RasterMoransI(chess, Square(1)))
    assert(r.getDouble(0, 0) === (-1.0 / 3))
    assert(r.getDouble(1, 0) === -0.2)
    assert(r.getDouble(0, 1) === -0.2)
    assert(r.getDouble(1, 1) === 0.0)
  }
}
