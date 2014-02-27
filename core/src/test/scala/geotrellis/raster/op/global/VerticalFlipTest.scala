package geotrellis.raster.op.global


import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.testutil._

import org.scalatest.FunSuite

class VerticalFlipTest extends FunSuite 
                          with TestServer {
  test("load valid raster") {
    val op1 = io.LoadRaster("test:fs","quadborder")
    val op2 = VerticalFlip(op1)
    val op3 = VerticalFlip(op2)

    val r1 = get(op1)
    val r2 = get(op2)
    val r3 = get(op3)

    assert(r1 === r3)

    for (y <- 0 until 20; x <- 0 until 20) {
      val y2 = 19 - y
      assert(r1.get(x, y) === r2.get(x, y2))
    }
  }
}
