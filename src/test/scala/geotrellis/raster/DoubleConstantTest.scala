package geotrellis.raster

import geotrellis._

import org.scalatest.FunSuite

class DoubleConstantTest extends FunSuite {
  val size = 4

  test("building") {
    val d1 = DoubleConstant(99.0, 2, 2)
    val d2 = DoubleArrayRasterData(Array.fill(size)(99.0), 2, 2)
    assert(d1 === d2)
  }

  test("basic operations") {
    val d = DoubleConstant(99.0, 2, 2)

    assert(d.length === size)
    assert(d.getType === TypeDouble)
    assert(d(0) === 99.0)
    assert(d.applyDouble(0) === 99.0)
  }

  test("map") {
    val d1 = DoubleConstant(99.0, 2, 2)
    val d2 = d1.mapDouble(_ + 1.0)

    assert(d2.isInstanceOf[DoubleConstant])
    assert(d2(0) === 100.0)
  }
}
