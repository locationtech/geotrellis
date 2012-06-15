package geotrellis.data

import geotrellis._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ArgTest extends FunSuite {
  var r:Raster = null

  var d:MutableRasterData = null

  test("create a float32 raster of zeros") {
    d = FloatArrayRasterData.ofDim(100)
    val e = Extent(0.0, 0.0, 10.0, 10.0)
    val re = RasterExtent(e, 1.0, 1.0, 10, 10)
    r = Raster(d, re)
  }

  test("make sure it contains 100 cells") {
    assert(r.length === 100)
  }

  test("make sure it's an array of zeros") {
    assert(d.toArrayDouble === Array.fill(100)(0.0))
  }

  test("update raster.data(3)") {
    assert(d.applyDouble(3) === 0.0)
    d.updateDouble(3, 99.0)
    assert(d.applyDouble(3) === 99.0)
  }

  test("update all raster values") {
    for (i <- 0 until 100) d.updateDouble(i, i.toDouble)
  }

  test("map over raster values") {
    val data2 = d.mapDouble(_ % 3.0)
    assert(data2.applyDouble(0) === 0.0)
    assert(data2.applyDouble(1) === 1.0)
    assert(data2.applyDouble(2) === 2.0)
    assert(data2.applyDouble(3) === 0.0)
    assert(data2(0) === 0)
  }

  // TODO: actually create and read a real float32 or float64 arg
}
