package geotrellis.raster

import geotrellis._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IntConstantTest extends FunSuite {
  val size = 4

  test("building") {
    val d1 = IntConstant(99, 2, 2)
    val d2 = IntArrayRasterData(Array.fill(size)(99), 2, 2)
    assert(d1 === d2)
  }

  test("basic operations") {
    val d = IntConstant(99, 2, 2)

    assert(d.length === size)
    assert(d.getType === TypeInt)
    assert(d(0) === 99)
    assert(d.applyDouble(0) === 99.0)
  }

  test("map") {
    val d1 = IntConstant(99, 2, 2)
    val d2 = d1.map(_ + 1)

    assert(d2.isInstanceOf[IntConstant])
    assert(d2(0) === 100)
  }
}
