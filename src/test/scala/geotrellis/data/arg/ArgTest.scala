package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis._
import geotrellis.testutil._

import geotrellis.process._
import geotrellis.raster._

import org.scalatest.FunSuite

// TODO

//@RunWith(classOf[JUnitRunner])
// class ArgTest extends FunSuite {
//   val server = TestServer.server

//   val data = IntArrayRasterData(Array(NODATA, -1, 2, -3,
//                                       4, -5, 6, -7,
//                                       8, -9, 10, -11,
//                                       12, -13, 14, -15), 4, 4)
//   val e = Extent(10.0, 11.0, 14.0, 15.0)
//   val re = RasterExtent(e, 1.0, 1.0, 4, 4)
//   val raster = Raster(data, re)

//   def loadRaster(path:String) = server.get(io.LoadFile(path))
//   def loadRasterData(path:String) = loadRaster(path).data.asArray

//   test("test float compatibility") {
//     assert(isNoData(data.applyDouble(0)))
//     assert(data.apply(0) === NODATA)

//     assert(data.applyDouble(1) === -1.0)
//     assert(data.apply(1) === -1)
//   }

//   test("write all supported arg datatypes") {
//     ArgWriter(TypeByte).write("/tmp/foo-int8.arg", raster, "foo-int8")
//     ArgWriter(TypeShort).write("/tmp/foo-int16.arg", raster, "foo-int16")
//     ArgWriter(TypeInt).write("/tmp/foo-int32.arg", raster, "foo-int32")
//     ArgWriter(TypeFloat).write("/tmp/foo-float32.arg", raster, "foo-float32")
//     ArgWriter(TypeDouble).write("/tmp/foo-float64.arg", raster, "foo-float64")
//   }

//   test("check int8") {
//     assert(loadRasterData("/tmp/foo-int8.arg") === data)
//     val l = loadRaster("/tmp/foo-int8.arg")
//     println(l.asciiDraw)
//   }

//   test("check int16") {
//     assert(loadRasterData("/tmp/foo-int16.arg") === data)
//   }

//   test("check int32") {
//     assert(loadRasterData("/tmp/foo-int32.arg") === data)
//   }

//   test("check float32") {
//     val d = loadRasterData("/tmp/foo-float32.arg")
//     assert(isNoData(d.applyDouble(0)))
//     assert(d.applyDouble(1) === -1.0)
//     assert(d.applyDouble(2) === 2.0)
//   }

//   test("check float64") {
//     val d = loadRasterData("/tmp/foo-float64.arg")
//     assert(isNoData(d.applyDouble(0)))
//     assert(d.applyDouble(1) === -1.0)
//     assert(d.applyDouble(2) === 2.0)
//   }

//   test("check c# test") {
//     var xmin = 0
//     var ymin = 0
//     var xmax = 15
//     var ymax = 11

//     var cellwidth = 1.5
//     var cellheight = 1.0

//     var cols = 10
//     var rows = 11
//     var A = 50.toByte
//     var o = -128.toByte
//     val byteArr:Array[Byte] = Array[Byte](
//       o,o,o,o,o,o,o,o,o,o, 
//       o,o,o,o,o,o,o,o,o,o, 
//       o,o,o,o,A,A,o,o,o,o,
//       o,o,o,A,o,o,A,o,o,o,
//       o,o,A,o,o,o,o,A,o,o,
//       o,A,o,o,o,o,o,o,A,o,
//       A,A,A,A,A,A,A,A,A,A,
//       o,o,o,o,o,o,o,o,o,o,
//       o,o,o,o,o,o,o,o,o,o,
//       o,o,o,o,o,o,o,o,o,o,
//       o,A,A,A,A,A,A,A,A,o)

//     val data = ByteArrayRasterData(byteArr, cols, rows)
//     val e = Extent(xmin, ymin, xmax, ymax)
//     val re = RasterExtent(e, cellwidth, cellheight, cols, rows)
//     val raster = Raster(data, re)
//     ArgWriter(TypeByte).write("/tmp/fooc-int8.arg", raster, "foo-int8")
//     val r2 = loadRaster("/tmp/fooc-int8.arg")
//     println(raster.asciiDraw)
//     println(r2.asciiDraw)
//     assert(r2.data === raster.data)
//   }
// }
