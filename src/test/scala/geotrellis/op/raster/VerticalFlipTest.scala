package geotrellis.op


import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op.VerticalFlip

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VerticalFlipTest extends FunSuite {
  val server = TestServer("src/test/resources/catalog.json")

  test("load valid raster") {
    val op1 = io.LoadRaster("quadborder")
    val op2 = VerticalFlip(op1)
    val op3 = VerticalFlip(op2)

    val r1 = server.run(op1)
    val r2 = server.run(op2)
    val r3 = server.run(op3)

    assert(r1 === r3)

    for (y <- 0 until 20; x <- 0 until 20) {
      val y2 = 19 - y
      assert(r1.get(x, y) === r2.get(x, y2))
    }
  }
}
