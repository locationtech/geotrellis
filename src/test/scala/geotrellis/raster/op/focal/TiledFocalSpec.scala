package geotrellis.raster.op.focal

import geotrellis._
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
class TiledZonalSpec extends FunSpec with FocalOpSpec
                              with ShouldMatchers
                              with TestServer {
  describe("normal min") {
    it("square min r=1") {
      val r = createRaster((0 until 16).toArray)
      assertEqual(Min(r, Square(1)), Array(0, 0, 1, 2,
                                           0, 0, 1, 2,
                                           4, 4, 5, 6,
                                           8, 8, 9, 10))
    }
  }
  describe("Min on a tiled raster") {

    it("square min r=1") {
      val r = createRaster((0 until 16).toArray)
      val tiledR = Tiler.createTiledRaster(r, 2, 2)
      val tileZonalOp = TileZonalOp(tiledR, Min(_, Square(1)))
      assertEqual(tileZonalOp, Array(0, 0, 1, 2,
                                     0, 0, 1, 2,
                                     4, 4, 5, 6,
                                     8, 8, 9, 10))
    }

  }
}
