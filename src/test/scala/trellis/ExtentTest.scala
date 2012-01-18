package trellis

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExtentTest extends FunSuite {
  test("invalid ranges") {
    intercept[ExtentRangeError] { Extent(10.0, 0.0, 0.0, 10.0) }
    intercept[ExtentRangeError] { Extent(0.0, 10.0, 10.0, 0.0) }
  }

  test("comparing extents") {
    val e1 = Extent(0.0, 0.0, 10.0, 10.0)
    val e2 = Extent(0.0, 20.0, 10.0, 30.0)
    val e3 = Extent(20.0, 0.0, 30.0, 10.0)
    val e4 = Extent(0.0, 0.0, 20.0, 20.0)
    val e5 = Extent(0.0, 0.0, 10.0, 30.0)

    assert((e1 compare e1) === 0)
    assert((e1 compare e2) === -1)
    assert((e1 compare e3) === -1)
    assert((e1 compare e4) === -1)
    assert((e1 compare e5) === -1)

    assert((e2 compare e1) === 1)
    assert((e2 compare e2) === 0)
    assert((e2 compare e3) === 1)
    assert((e2 compare e4) === 1)
    assert((e2 compare e5) === 1)

    assert((e3 compare e1) === 1)
    assert((e3 compare e2) === -1)
    assert((e3 compare e3) === 0)
    assert((e3 compare e4) === 1)
    assert((e3 compare e5) === 1)

    assert((e4 compare e1) === 1)
    assert((e4 compare e2) === -1)
    assert((e4 compare e3) === -1)
    assert((e4 compare e4) === 0)
    assert((e4 compare e5) === -1)

    assert((e5 compare e1) === 1)
    assert((e5 compare e2) === -1)
    assert((e5 compare e3) === -1)
    assert((e5 compare e4) === 1)
    assert((e5 compare e5) === 0)
  }

  test("combining extents") {
    val e1 = Extent(0.0, 0.0, 10.0, 10.0)
    val e2 = Extent(20.0, 0.0, 30.0, 10.0)
    val e3 = Extent(0.0, 0.0, 30.0, 10.0)
    assert(e1.combine(e2) === e3)
  }

  test("contains interior points") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(3.0, 3.0) === true)
    assert(e.containsPoint(0.00001, 9.9999) === true)
  }

  test("doesn't contain exterior points") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(100.0, 0.0) === false)
    assert(e.containsPoint(0.0, 1000.0) === false)
  }

  test("doesn't contain boundary") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.containsPoint(0.0, 0.0) === false)
    assert(e.containsPoint(0.0, 3.0) === false)
    assert(e.containsPoint(0.0, 10.0) === false)
    assert(e.containsPoint(10.0, 0.0) === false)
    assert(e.containsPoint(10.0, 10.0) === false)
  }

  test("get corners") {
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    assert(e.southWest === (0.0, 0.0))
    assert(e.northEast === (10.0, 10.0))
  }
}
