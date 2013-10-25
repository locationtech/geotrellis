package geotrellis.raster

import geotrellis._

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

// TODO

// @RunWith(classOf[JUnitRunner])
// class CroppedRasterSpec extends FunSuite {
//   val (cols, rows) = (5, 5)
//   val (cw, ch) = (20.0, 20.0)
//   val (xmin, ymin) = (0.0, 0.0)
//   val (xmax, ymax) = (xmin + cw * cols, ymin + ch * rows)

//   val a = (1 to cols * rows).toArray
//   val d = IntArrayRasterData(a, cols, rows)
//   val e = Extent(xmin, ymin, xmax, ymax)
//   val re = RasterExtent(e, cw, ch, cols, rows)
//   val r = Raster(d, re)

//   val tiledRaster = Tiler.createTiledRaster(r, 2, 2)
  
//   def ints(a:Array[Int], cols:Int, rows:Int) = IntArrayRasterData(a, cols, rows)

//   test("aligned crop") {
//     val r2 = CroppedRaster(r, Extent(40.0, 20.0, 80.0, 40.0))
//     assert(r2.data === ints(Array(18, 19), 2, 1))
//     assert(r2.get(0,0) == 18)
//     assert(r2.get(1,0) == 19)
//   }

//   test("aligned crop on tiled raster") {
//     val r2 = CroppedRaster(tiledRaster, Extent(40.0, 20.0, 80.0, 40.0))
//     assert(r2.get(0,0) == 18)
//     assert(r2.get(1,0) == 19)
//     val data = r2.data.asInstanceOf[CroppedTiledRasterData]
//     assert( data != null)
//   }

//   test("slightly smaller crop") {
//     val r2 = CroppedRaster(r, Extent(40.0, 20.0, 80.0, 40.0))
//     val r3 = CroppedRaster(r, Extent(40.1, 20.1, 79.99, 39.99))
//     assert(r2 === r3)
//   }
  
//   test("slightly smaller crop on tiled raster") {
//     val r2 = CroppedRaster(tiledRaster, Extent(40.0, 20.0, 80.0, 40.0))
//     val r3 = CroppedRaster(tiledRaster, Extent(40.1, 20.1, 79.99, 39.99))
//     assert(r2 === r3)
//   }
  
//   test("slightly bigger crop") {
//     val r2 = CroppedRaster(r, Extent(40.0, 20.0, 80.0, 40.0))
//     val r3 = CroppedRaster(r, Extent(39.9, 19.9, 80.01, 40.1))
//     val r4 = CroppedRaster(r, Extent(20.0, 0.0, 100.0, 60.0))
//     assert(r2 != r3)
//     assert(r4 === r3)
//   }
  
//   test("slightly bigger crop on tiled raster") {
//     val r2 = CroppedRaster(tiledRaster, Extent(40.0, 20.0, 80.0, 40.0))
//     val r3 = CroppedRaster(tiledRaster, Extent(39.9, 19.9, 80.01, 40.1))
//     val r4 = CroppedRaster(tiledRaster, Extent(20.0, 0.0, 100.0, 60.0))
//     assert(r2 != r3)
//     assert(r4 === r3)
//   }
// }
