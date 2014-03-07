package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.raster.op._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class GetHistogramSpec extends FunSpec 
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

  describe("GetHistogram") {
    it("should get correct values from test raster.") {
      val histo = get(GetHistogram(testRaster))

      histo.getTotalCount should be (18)
      histo.getItemCount(11) should be (2)
      histo.getItemCount(12) should be (3)

      histo.getQuantileBreaks(4) should be (Array(12, 15, 66, 95))
    }
  }
}
