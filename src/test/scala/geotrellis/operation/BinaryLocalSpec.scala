package geotrellis.operation

import geotrellis.process._
import geotrellis.operation._
import geotrellis.raster._
import geotrellis._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class BinaryLocalSpec extends Spec with MustMatchers with ShouldMatchers {
  def f(op:Op[IntRaster]) = AddConstant(op, 1)

  describe("The BinaryLocal operation (Subtract)") {
    val cols = 100
    val rows = 100

    val server = TestServer()
    val e = Extent(0.0, 0.0, 100.0, 100.0)
    val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)

    def makeData(c:Int) = Array.fill(re.cols * re.rows)(c)
    def makeRaster(c:Int) = IntRaster(makeData(c), re)

    val r63 = makeRaster(63)
    val r46 = makeRaster(46)
    val r33 = makeRaster(33)
    val r17 = makeRaster(17)
    val r13 = makeRaster(13)

    it("should produce correct results") {
      val r = server.run(Subtract(r63, r17))
      r.get(0, 0) must be === r46.get(0, 0)
    }

    it("should collapse operations") {
      val a = AddConstant(MultiplyConstant(AddConstant(r13, 1), 3), 5)
      val b = AddConstant(MultiplyConstant(AddConstant(r13, 1), 2), 3)
      val op = Subtract(a, b)

      val Complete(r, history) = server.getResult(op)
      println(history.toPretty)

      r.get(0, 0) must be === 16
    }
  }
}
