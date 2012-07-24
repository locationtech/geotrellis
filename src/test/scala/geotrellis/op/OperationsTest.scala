package geotrellis.op

import geotrellis._
import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.Implicits._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class OperationsTest extends FunSuite {
  val server = TestServer()

  def run(prefix:String, op:Op[Raster], expected:Raster) {
    test("%s:%s" format (prefix, op.name)) {
      val got = server.run(op)
      if (got != expected) {
        println("got " + got.data.asInstanceOf[ArrayRasterData].applyDouble(0))
        println("expected " + expected.data.asInstanceOf[ArrayRasterData].applyDouble(0))
      }
      assert(got == expected)
    }
    //n += 1
  }

  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val r1 = Raster(Array.fill(100)(1), re)
  val r2 = Raster(Array.fill(100)(2), re)
  val r3 = Raster(Array.fill(100)(3), re)
  val r6 = Raster(Array.fill(100)(6), re)
  val r9 = Raster(Array.fill(100)(9), re)
  
  run("int", AddConstant(r3, 6), r9)

  run("int", BitwiseAndConstant(r9, 3), r1)

  run("int", DivideConstant(r9, 3), r3)
  run("int", DivideDoubleConstant(r9, 3.0), r3)
  run("int", DivideConstantBy(18, r9), r2)
  run("int", DivideDoubleConstantBy(18.0, r9), r2)

  run("int", DoCell(r9, _ - 3), r6)

  run("int", MultiplyConstant(r2, 3), r6)
  run("int", MultiplyDoubleConstant(r2, 3.0), r6)

  run("int", PowConstant(r3, 2), r9)
  run("int", PowDoubleConstant(r9, 0.5), r3)

  run("int", r2 + r1, r3)
  run("int", r2 * r3, r6)
  run("int", r9 / r3, r3)
  run("int", r3 - r2, r1)


  // doubles

  val d1_1 = Raster(Array.fill(100)(1.1), re)
  val d2_2 = Raster(Array.fill(100)(2.2), re)
  val d3_3 = Raster(Array.fill(100)(3.3), re)
  val d6_6 = Raster(Array.fill(100)(6.6), re)
  val d9_9 = Raster(Array.fill(100)(9.9), re)

  val d1 = Raster(Array.fill(100)(1.0), re)
  val d3 = Raster(Array.fill(100)(3.0), re)
  val d7 = Raster(Array.fill(100)(7.0), re)
  val d9 = Raster(Array.fill(100)(9.0), re)
  val d10 = Raster(Array.fill(100)(10.0), re)
  val d11 = Raster(Array.fill(100)(11.0), re)

  run("double", AddConstant(d3, 6), d9)

  run("double", BitwiseAndConstant(d9, 3), d1)
  
  run("double", DivideConstant(d9_9, 3), d3_3)
  run("double", DivideDoubleConstant(d9_9, 3.0), d3_3)
  run("double", DivideConstantBy(99, d9_9), d10)
  run("double", DivideDoubleConstantBy(99.0, d9_9), d10)
  
  run("double", DoCell(d10, _ - 3), d7)
  
  run("double", MultiplyConstant(d2_2, 3), d6_6)
  run("double", MultiplyDoubleConstant(d10, 1.1), d11)
  
  run("double", PowConstant(d3, 2), d9)
  run("double", PowDoubleConstant(d9, 0.5), d3)
  
  run("double", d2_2 + d1_1, d3_3)
  run("double", d2_2 * d3, d6_6)
  run("double", d9_9 / d3_3, d3_3)
  run("double", d3_3 - d2_2, d1_1)
}
