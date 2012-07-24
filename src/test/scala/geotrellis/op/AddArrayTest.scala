package geotrellis.op

import geotrellis._
import geotrellis.process._
import geotrellis.raster._
import geotrellis.raster.op.local.AddArray

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class AddArrayTest extends FunSuite {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val re = RasterExtent(e, 1.0, 1.0, 10, 10)

  val server = TestServer()

  def r(n:Int) = Raster(Array.fill(100)(n), re)
  def r(n:Double) = Raster(Array.fill(100)(n), re)

  def addInts(ns:Int*) = AddArray(ns.map(n => r(n)).toArray.asInstanceOf[Array[Raster]])
  def addDoubles(ns:Double*) = AddArray(ns.map(n => r(n)).toArray.asInstanceOf[Array[Raster]])

  test("add integers") {
    val a = 3
    val b = 6
    val c = 9
    val n = NODATA

    assert(server.run(addInts(a, b)) === r(c))
    assert(server.run(addInts(n, b)) === r(b))
    assert(server.run(addInts(c, n)) === r(c))
    assert(server.run(addInts(n, n)) === r(n))
  }

  test("add doubles") {
    val a = 3000000000.0
    val b = 6000000000.0
    val c = 9000000000.0
    val x = a + a + b + b + c
    val n = Double.NaN

    assert(server.run(addDoubles(a, b)) === r(c))
    assert(server.run(addDoubles(n, b)) === r(b))
    assert(server.run(addDoubles(c, n)) === r(c))
    assert(server.run(addDoubles(n, n)) === r(n))

    assert(server.run(addDoubles(a, a, b, b, c)) === r(x))
  }
}
