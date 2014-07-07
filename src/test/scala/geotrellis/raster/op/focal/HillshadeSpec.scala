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

import spire.syntax.cfor._

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

  test("Hillshade works with raster source") {
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

  test("should run Hillshade square 3 on tiled raster in catalog") {
    val name = "SBN_inc_percap"

    val source = RasterSource(name)


    val r = source.get
    val tileLayout = TileLayout.fromTileDimensions(r.rasterExtent,256,256)
    val rs = RasterSource(TileRaster.wrap(r,tileLayout,cropped = false))

    val expected = source.focalHillshade.get
    rs.focalHillshade.run match {
      case Complete(value,hist) =>
        // Dont check last col or last row. 
        // Reason is, because the offsetting of the tiles, the tiled version
        // pulls in NoData's where the non tiled just has the edge value,
        // so the calculations produce different results. Which makes sense.
        // So if your going for accuracy don't tile something that create NoData 
        // borders.
        cfor(0)(_ < expected.cols-1, _ + 1) { col =>
          cfor(0)(_ < expected.rows-1, _ + 1) { row =>
            withClue (s"Value different at $col,$row: ") {
              val v1 = value.getDouble(col,row)
              val v2 = expected.getDouble(col,row)
              if(isNoData(v1)) isNoData(v2) should be (true)
              else if(isNoData(v2)) isNoData(v1) should be (true)
              else v1 should be (v2)
            }
          }
        }
      case Error(message,trace) =>
        println(message)
        assert(false)
    }
  }
}
