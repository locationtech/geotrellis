package trellis.operation

import trellis.process._
import trellis.operation._
import trellis.raster._
import trellis._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UnaryLocalSpec extends Spec with MustMatchers with ShouldMatchers {
  describe("The UnaryLocal operation (AddConstant)") {

    val cols = 1000
    val rows = 1000

    val server = TestServer()
    val e = Extent(0.0, 0.0, 100.0, 100.0)
    val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)
    val data = Array.fill(re.cols * re.rows)(100)
    val raster = IntRaster(data, re.cols, re.rows, re)

    it("should produce correct results") {
      val op = AddConstant(raster, 33)
      val raster2 = server.run(op)
      raster2.data(0) must be === raster.data(0) + 33
    }

    it("should compose multiple operations") {
      val f = (op:Operation[IntRaster]) => AddConstant(op, 1)
      val op = f(f(f(f(raster)))) // should add 4
      //val op = f(raster) // should add 1
      val Complete(raster2, history) = server.getResult(op)
      raster2.data(0) must be === raster.data(0) + 4
      //raster2.data(0) must be === raster.data(0) + 1
      println(history.toPretty)
    }
  }
}
