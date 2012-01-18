package trellis.operation

import trellis._
import trellis.process._
import trellis.raster._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AddTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val r1 = IntRaster(Array.fill(100)(3), 10, 10, re)
  val r2 = IntRaster(Array.fill(100)(6), 10, 10, re)
  val r3 = IntRaster(Array.fill(100)(9), 10, 10, re)

  val server = TestServer()

  test("add correctly") {
    assert(server.run(Add(r1, r2)) === r3)
    //val rr = server.run(Add(r1, r2))
    //println("%s %s %s %s" format (rr.rasterExtent, rr.cols, rr.rows, rr.data.asList))
    //println("%s %s %s %s" format (r3.rasterExtent, r3.cols, r3.rows, r3.data.asList))
  }
}
