package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.raster._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class MoranSpec extends FunSpec with TestServer {
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

  describe("ScalarMoransI") {
    it("computes square moran (chess)") {
      val n = run(ScalarMoransI(chess, Nesw(1)))
      assert(n === -1.0)
    }

    it("computes diagonal moran (chess)") {
      val n = run(ScalarMoransI(chess, Square(1)))
      assert(n === (-2.0 / 30))
    }
  }

  describe("RasterMoransI") {
    it("computes square moran (chess)") {
      val r = run(RasterMoransI(chess, Nesw(1)))
      assert(r.toArrayDouble === Array.fill(64)(-1.0))
    }
    
    it("computes diagonal moran (chess)") {
      val r = run(RasterMoransI(chess, Square(1)))
      assert(r.getDouble(0, 0) === (-1.0 / 3))
      assert(r.getDouble(1, 0) === -0.2)
      assert(r.getDouble(0, 1) === -0.2)
      assert(r.getDouble(1, 1) === 0.0)
    }
  }
}
