package geotrellis.raster

import geotrellis._
import geotrellis.data.RasterReader

import org.scalatest.FunSuite

// TODO
// @RunWith(classOf[JUnitRunner])
// class ResampledArrayRasterDataTest extends FunSuite {
//   val (cols, rows) = (5, 5)
//   val (cw, ch) = (20.0, 20.0)
//   val (xmin, ymin) = (0.0, 0.0)
//   val (xmax, ymax) = (xmin + cw * cols, ymin + ch * rows)

//   val a = (1 to cols * rows).toArray
//   val d = IntArrayRasterData(a, cols, rows)
//   val e = Extent(xmin, ymin, xmax, ymax)
//   val src = RasterExtent(e, cw, ch, cols, rows)
//   val r = Raster(d, src)
//   //println(r.asciiDraw)

//   def ints(a:Array[Int], cols:Int, rows:Int) = {
//     IntArrayRasterData(a, cols, rows)
//   }

//   def resample(d:RasterData, src:RasterExtent, dst:RasterExtent) = {
//     Raster(ResampledArrayRasterData(d, src, dst), dst)
//   }

//   def resample2(d:RasterData, src:RasterExtent, dst:RasterExtent) = {
//     RasterReader.read(Raster(d, src), Option(dst))
//   }

//   test("noop resample") {
//     val dst = src
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === d)
//     assert(rr === resample2(d, src, dst))
//   }

//   test("crop via resample") {
//     val dst = RasterExtent(Extent(0.0, 0.0, 40.0, 40.0), cw, ch, 2, 2)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(16, 17, 21, 22), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }

//   test("distortion via resample") {
//     val dst = RasterExtent(src.extent, 100.0 / 3, 100.0 / 3, 3, 3)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(1, 3, 5, 11, 13, 15, 21, 23, 25), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }

//   test("northeast of src") {
//     val dst = RasterExtent(Extent(200.0, 200.0, 300.0, 300.0), 50.0, 50.0, 2, 2)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }

//   test("southwest of src") {
//     val dst = RasterExtent(Extent(-100, -100, 0.0, 0.0), 50.0, 50.0, 2, 2)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(NODATA, NODATA, NODATA, NODATA), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }

//   test("partially northeast of src") {
//     val dst = RasterExtent(Extent(50.0, 50.0, 150.0, 150.0), 50.0, 50.0, 2, 2)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(NODATA, NODATA, 9, NODATA), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }

//   test("partially southwest of src") {
//     val dst = RasterExtent(Extent(-50.0, -50.0, 50.0, 50.0), 50.0, 50.0, 2, 2)
//     val rr = resample(d, src, dst)
//     //println(rr.asciiDraw)
//     assert(rr.rasterExtent === dst)
//     assert(rr.data === ints(Array(NODATA, 17, NODATA, NODATA), 2, 2))
//     assert(rr === resample2(d, src, dst))
//   }
// }
