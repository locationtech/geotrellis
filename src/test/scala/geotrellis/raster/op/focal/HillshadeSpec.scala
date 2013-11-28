package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.source._
import geotrellis.raster.op._
import geotrellis.statistics.op._
import geotrellis.process._
import geotrellis.data._
import geotrellis.testutil._
import geotrellis.raster._
import geotrellis.render._
import geotrellis.render.op._

import org.scalatest.FunSuite

class HillshadeSpec extends FunSuite 
                       with TestServer 
                       with RasterBuilders {
  def grayscale(n:Int) = {
    val ns = (1 to 128).toArray
    val limits = ns.map(i => i * n)
    val colors = ns.map(i => ((i * 65536 * 2 + i * 256 * 2 + i * 2) << 8) | 255)
    ColorBreaks(limits, colors)
  }

  def time() = System.currentTimeMillis()

  // for more information on how hillshade work, see: http://bit.ly/Qj0YPg.
  // note that we scale by 128 not 256, so our result is 77 instead of 154.

  test("esri hillshade") {
    val re = RasterExtent(Extent(0.0, 0.0, 25.0, 25.0), 5.0, 5.0, 5, 5)
    val arr = Array(0, 0, 0, 0, 0,
                    0, 2450, 2461, 2483, 0,
                    0, 2452, 2461, 2483, 0,
                    0, 2447, 2455, 2477, 0,
                    0, 0, 0, 0, 0)
    val data = IntArrayRasterData(arr, 5, 5)
    val r = Raster(data, re)

    val h = get(Hillshade(Aspect(r),Slope(r,1.0),315.0,45.0))
    val h2 = get(Hillshade(r,315.0,45.0,1.0))
    assert(h.get(2, 2) === 77)
    assert(h2.get(2,2) === 77)
  }

  test("can write hillshade") {
    val path = "src/test/resources/sbn/SBN_inc_percap.arg"

    val grayBreaks = grayscale(1)
  
    println("Starting test Hillshade operation.")
    println("Loading raster")
  
    val t0 = time()
    val r = get(io.LoadFile(path))
    val h = get(stat.GetHistogram(r))
    println("loaded raster in %s ms" format (time() - t0))
  
    get(io.WritePng(r, "/tmp/raster.png", grayscale(6), 0))
  
    val t1 = time()
    val r1 = get(Hillshade(r))
    println("generated hillshade from memory in %s ms" format (time() - t1))

    val h1 = get(stat.GetHistogram(r1))

    val t2 = time()
    get(io.WritePng(r1, "/tmp/hillshade2.png", grayBreaks, 0))
    println("[2] wrote png in %s ms" format (time() - t2))

    val palette = Array(0xff0000ff, 0xff8800ff, 0xffff00ff,
                        0x00ff00ff, 0x00ffffff, 0x0000ffff)

    val colors10 = new MultiColorRangeChooser(palette).getColors(10)
    val colors100 = new MultiColorRangeChooser(palette).getColors(100)
    val colors1000 = new MultiColorRangeChooser(palette).getColors(1000)
    val colors638 = new MultiColorRangeChooser(palette).getColors(638)

    val t3 = time()
    get(io.WritePng(r, "/tmp/raster3.png", GetColorBreaks(h, colors10), 0))
    println("[3] wrote png in %s ms" format (time() - t3))

    val t4 = time()
    get(io.WritePng(r, "/tmp/raster4.png", GetColorBreaks(h, colors100), 0))
    println("[4] wrote png in %s ms" format (time() - t4))

    val t5 = time()
    get(io.WritePng(r, "/tmp/raster5.png", GetColorBreaks(h, colors1000), 0))
    println("[5] wrote png in %s ms" format (time() - t5))

    val t6 = time()
    get(io.WritePng(r, "/tmp/raster6.png", GetColorBreaks(h, colors638), 0))
    println("[6] wrote png in %s ms" format (time() - t6))
  }

  test("Hillshade works with raste source") {
    val rs = createRasterSource(
      Array(0, 0, 0,              0, 0, 0,
            0, 2450, 2461,        2483, 0, 0,

            0, 2452, 2461,        2483, 0, 0,
            0, 2447, 2455,        2477, 0, 0),
      2,2,3,2,5,5)

    run(rs.focalHillshade(315.0,45.0,1.0)) match {
      case Complete(result,success) =>
        //          println(success)
        printR(result)
        assert(result.get(2, 2) === 77)
      case Error(msg,failure) =>
        println(msg)
        println(failure)
        assert(false)
    }
  }

  test("should get the same result for split raster") {
    val rOp = getRaster("elevation")
    val nonTiledSlope = Hillshade(rOp,315.0,45.0,1.0)

    val tiled =
      rOp.map { r =>
        val (tcols,trows) = (11,20)
        val pcols = r.rasterExtent.cols / tcols
        val prows = r.rasterExtent.rows / trows
        val tl = TileLayout(tcols,trows,pcols,prows)
        TileRaster.wrap(r,tl)
      }

    val rs = RasterSource(tiled)
    run(rs.focalHillshade(315.0,45.0,1.0)) match {
      case Complete(result,success) =>
        //          println(success)
        assertEqual(result,nonTiledSlope)
      case Error(msg,failure) =>
        println(msg)
        println(failure)
        assert(false)
    }
  }
}
