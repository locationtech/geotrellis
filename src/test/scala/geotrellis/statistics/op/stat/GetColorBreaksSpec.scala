package geotrellis.statistics.op.stat

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GetColorBreaksSpec extends FunSpec 
                            with TestServer
                            with ShouldMatchers {
  def testRaster = {
    val rasterExtent = RasterExtent(Extent(0.0, 0.0, 100.0, 80.0), 20.0, 20.0, 5, 4)
    val nd = NODATA
    val data1 = Array(12, 12, 13, 14, 15,
                      44, 91, nd, 11, 95,
                      12, 13, 56, 66, 66,
                      44, 91, nd, 11, 95)
    Raster(data1, rasterExtent)
  }

  describe("GetColorBreaks") {
    it("gets color breaks for test raster.") {
      val h = GetHistogram(Literal(testRaster), 101)
      val (g, y, o, r) = (0x00ff00ff, 0xffff00ff, 0xff7f00ff, 0xff0000ff)
      val colors = Array(g, y, o, r)
      val colorBreaks = run(GetColorBreaks(h, colors))
      colorBreaks.limits should be (Array(12, 15, 66, 95))
      colorBreaks.colors should be (Array(g, y, o, r))
    }
  }
}
