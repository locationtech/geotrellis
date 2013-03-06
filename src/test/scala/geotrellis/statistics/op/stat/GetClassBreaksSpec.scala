package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.io.LoadFile

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GetClassBreaksSpec extends FunSpec 
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

  describe("GetClassBreaks") {
    it("gets expected class breaks from test raster.") {
      val h = GetHistogram(Literal(testRaster), 101)
      val result = GetClassBreaks(h, 4)
      server.run(result) should be (Array(12, 15, 66, 95))
    }
  }
}
