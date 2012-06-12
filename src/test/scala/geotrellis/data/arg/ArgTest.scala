package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.operation._
import geotrellis.process._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ArgTest extends FunSuite {
  val server = TestServer()

  val data = IntArrayRasterData(Array(NODATA, -1, 2, -3,
                                      4, -5, 6, -7,
                                      8, -9, 10, -11,
                                      12, -13, 14, -15))
  val e = Extent(10.0, 11.0, 14.0, 15.0)
  val re = RasterExtent(e, 1.0, 1.0, 4, 4)
  val raster = Raster(data, re)

  def loadRaster(path:String) = server.run(LoadFile(path))
  def loadRasterData(path:String) = loadRaster(path).data.asArray

  test("test float compatibility") {
    assert(java.lang.Double.isNaN(data.applyDouble(0)))
    assert(data.apply(0) === NODATA)

    assert(data.applyDouble(1) === -1.0)
    assert(data.apply(1) === -1)
  }

  test("write all supported arg datatypes") {
    ArgWriter(TypeByte).write("/tmp/foo-int8.arg", raster, "foo-int8")
    ArgWriter(TypeShort).write("/tmp/foo-int16.arg", raster, "foo-int16")
    ArgWriter(TypeInt).write("/tmp/foo-int32.arg", raster, "foo-int32")
    ArgWriter(TypeFloat).write("/tmp/foo-float32.arg", raster, "foo-float32")
    ArgWriter(TypeDouble).write("/tmp/foo-float64.arg", raster, "foo-float64")
  }

  test("check int8") {
    assert(loadRasterData("/tmp/foo-int8.arg") === data)
  }

  test("check int16") {
    assert(loadRasterData("/tmp/foo-int16.arg") === data)
  }

  test("check int32") {
    assert(loadRasterData("/tmp/foo-int32.arg") === data)
  }

  test("check float32") {
    val d = loadRasterData("/tmp/foo-float32.arg")
    assert(java.lang.Double.isNaN(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }

  test("check float64") {
    val d = loadRasterData("/tmp/foo-float64.arg")
    assert(java.lang.Double.isNaN(d.applyDouble(0)))
    assert(d.applyDouble(1) === -1.0)
    assert(d.applyDouble(2) === 2.0)
  }
}
