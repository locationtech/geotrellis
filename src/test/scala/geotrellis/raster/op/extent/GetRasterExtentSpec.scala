package geotrellis.raster.op.extent

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GetRasterExtentSpec extends FunSpec 
                             with ShouldMatchers 
                             with TestServer {
  describe("GetRasterExtent") {
    it("should get a RasterExtent correctly.") {
      val ext = Extent(0.0,-10.0,100.0,-1.0)
      val cellWidth = 10
      val cellHeight = 1
      val cols = 10
      val rows = 9
      val expected = RasterExtent(ext,cellWidth,cellHeight,cols,rows)
      val actual = run(GetRasterExtent(0.0, -10.0, 100.0, -1.0, cols, rows))

      actual should be (expected)
    }

    it("should get another RasterExtent correctly") {
      val e = Extent(xmin = -90, ymin = 20,
                     xmax = -80, ymax = 40)
      val re = run(GetRasterExtent(e, 20, 30))
      re.extent.xmin should be (-90)
      re.cols should be (20)
    }
  }
}
