package geotrellis

import geotrellis._
import geotrellis.raster.op.local._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.Implicits._
import geotrellis.testutil._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers

@RunWith(classOf[JUnitRunner])
class OperationsTest extends FunSuite 
                        with TestServer {
  var counter = 1
  def run(prefix:String, op:Op[Raster], expected:Raster) {
    test("%s:%s:%s" format (prefix, op.name, counter)) {
      val got = run(op)
      if (got != expected) {
        println("got " + got.data.asInstanceOf[ArrayRasterData].applyDouble(0))
        println("expected " + expected.data.asInstanceOf[ArrayRasterData].applyDouble(0))
      }
      assert(got == expected)
    }
    counter += 1
  }

  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val rn10 = Raster(Array.fill(100)(-10), re)
  val r0 = Raster(Array.fill(100)(0), re)
  val r1 = Raster(Array.fill(100)(1), re)
  val r2 = Raster(Array.fill(100)(2), re)
  val r3 = Raster(Array.fill(100)(3), re)
  val r6 = Raster(Array.fill(100)(6), re)
  val r9 = Raster(Array.fill(100)(9), re)
  val r10 = Raster(Array.fill(100)(10), re)
  val r11 = Raster(Array.fill(100)(11), re)
  
  run("int", Xor(r9, 3), r10)
  run("int", Xor(9, r3), r10)
  run("int", Xor(r9, r3), r10)

  run("int", Not(r9), rn10)
  run("int", Defined(r9), r1)
  run("int", Undefined(r9), r0)

  run("int", Equal(r9, r9), r1)
  run("int", Equal(r9, r3), r0)

  run("int", Unequal(r9, r9), r0)
  run("int", Unequal(r9, r3), r1)

  run("int", DivideConstant(r9, 3), r3)
  run("int", DivideDoubleConstant(r9, 3.0), r3)
  run("int", DivideConstantBy(18, r9), r2)
  run("int", DivideDoubleConstantBy(18.0, r9), r2)

  run("int", DoCell(r9)(_ - 3), r6)

  run("int", MultiplyConstant(r2, 3), r6)
  run("int", MultiplyDoubleConstant(r2, 3.0), r6)

  run("int", PowConstant(r3, 2), r9)
  run("int", PowDoubleConstant(r9, 0.5), r3)

  run("int", r2 + r1, r3)
  run("int", r2 * r3, r6)
  run("int", r9 / r3, r3)
  run("int", r3 - r2, r1)

  // test Operation.into()
  run("int", Literal(r2).into(Multiply(_, 3)), r6) 

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

  run("double", DivideConstant(d9_9, 3), d3_3)
  run("double", DivideDoubleConstant(d9_9, 3.0), d3_3)
  run("double", DivideConstantBy(99, d9_9), d10)
  run("double", DivideDoubleConstantBy(99.0, d9_9), d10)
  
  run("double", DoCellDouble(d10)(_ - 3), d7)
  
  run("double", MultiplyConstant(d2_2, 3), d6_6)
  run("double", MultiplyDoubleConstant(d10, 1.1), d11)
  
  run("double", PowConstant(d3, 2), d9)
  run("double", PowDoubleConstant(d9, 0.5), d3)
  
  run("double", d2_2 + d1_1, d3_3)
  run("double", d2_2 * d3, d6_6)
  run("double", d9_9 / d3_3, d3_3)
  run("double", d3_3 - d2_2, d1_1)

  test("Collapsing operations") {
    // I'm not really sure what this test is doing.
    val cols = 100
    val rows = 100

    val e = Extent(0.0, 0.0, 100.0, 100.0)
    val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)

    def makeData(c:Int) = Array.fill(re.cols * re.rows)(c)
    def makeRaster(c:Int) = Raster(makeData(c), re)

    val r63 = makeRaster(63)
    val r46 = makeRaster(46)
    val r33 = makeRaster(33)
    val r17 = makeRaster(17)
    val r13 = makeRaster(13)

    val a = AddConstant(MultiplyConstant(AddConstant(r13, 1), 3), 5)
    val b = AddConstant(MultiplyConstant(AddConstant(r13, 1), 2), 3)
    val op = Subtract(a, b)

    val Complete(r, history) = getResult(op)
    r.get(0, 0) should be (16)
  }

  test("The UnaryLocal operation (AddConstant)") {
    def f(op:Op[Raster]) = AddConstant(op, 1)
    val cols = 1000
    val rows = 1000

    val e = Extent(0.0, 0.0, 100.0, 100.0)
    val re = RasterExtent(e, e.width / cols, e.height / rows, cols, rows)
    val data = Array.fill(re.cols * re.rows)(100)
    val raster = Raster(data, re)

    val d = raster.data.asArray.get

    val op = AddConstant(raster, 33)
    val raster2 = run(op)
    val d2 = raster2.data.asArray.get
    d2(0) should be (d(0) + 33)

    val d3 = run(f(f(raster))).data.asArray.get
    d3(0) should be (d(0) + 2)
    
    val d4 = run(f(f(f(raster)))).data.asArray.get
    d4(0) should be (d(0) + 3)

    val d5 = run(f(f(f(f(raster))))).data.asArray.get
    d5(0) should be (d(0) + 4)

    val Complete(raster3, history) = getResult(f(f(f(f(f(raster))))))
    val d6 = raster3.data.asArray.get
    d6(0) should be (d(0) + 5)
  }
}
