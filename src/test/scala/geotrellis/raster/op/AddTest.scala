package geotrellis.raster.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import geotrellis.raster.op.local.Add

@RunWith(classOf[JUnitRunner])
class AddTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val r1 = Raster(Array.fill(100)(3), re).defer
  val r2 = Raster(Array.fill(100)(6), re).defer
  val r3 = Raster(Array.fill(100)(9), re)

  val server = TestServer()

  test("add correctly") {
    val rr = server.run(Add(r1, r2))
    assert(server.run(Add(r1, r2)) === r3)
  }
}
