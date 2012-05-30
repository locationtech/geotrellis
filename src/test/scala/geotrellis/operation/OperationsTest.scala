package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.Implicits._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OperationsTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val r1 = IntRaster(Array.fill(100)(1), re)
  val r2 = IntRaster(Array.fill(100)(2), re)
  val r3 = IntRaster(Array.fill(100)(3), re)
  val r6 = IntRaster(Array.fill(100)(6), re)
  val r9 = IntRaster(Array.fill(100)(9), re)

  val server = TestServer()
  var n:Int = 1

  def run(op:Op[IntRaster], expected:IntRaster) {
    test("%d:%s" format (n, op.name)) {
      val got = server.run(op)
      //println("op " + op)
      //println("got " + got.data(0))
      //println("expected " + expected.data(0))
      assert(got == expected)
    }
    n += 1
  }
  
  run(AddConstant(r3, 6), r9)

  run(Bitmask(r9, 3), r1)

  run(DivideConstant(r9, 3), r3)
  run(DivideDoubleConstant(r9, 3.0), r3)
  run(DivideConstantBy(18, r9), r2)
  run(DivideDoubleConstantBy(18.0, r9), r2)

  run(DoCell(r9, _ - 3), r6)

  run(MultiplyConstant(r2, 3), r6)
  run(MultiplyDoubleConstant(r2, 3.0), r6)

  run(PowConstant(r3, 2), r9)
  run(PowDoubleConstant(r9, 0.5), r3)

  run(r2 + r1, r3)
  run(r2 * r3, r6)
  run(r9 / r3, r3)
  run(r3 - r2, r1)
}
