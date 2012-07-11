package geotrellis.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import local.AddArray

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AddArrayTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val r1 = Raster(Array.fill(100)(3), re)
  val r2 = Raster(Array.fill(100)(6), re)
  val r3 = Raster(Array.fill(100)(9), re)

  val server = TestServer()

  test("add correctly") {
    assert(server.run(AddArray(Literal(Array(r1, r2)))) === r3)
  }
}
