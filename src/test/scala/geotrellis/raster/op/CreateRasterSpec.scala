package geotrellis.raster.op

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class CreateRasterSpec extends FunSpec 
                          with ShouldMatchers 
                          with TestServer {
  describe("CreateRaster") {
    it("creates an empty raster.") {
      val ext = Extent(0.0,-10.0,100.0,-1.0)
      val cellWidth = 10
      val cellHeight = 1
      val cols = 10
      val rows = 9
      val extent = RasterExtent(ext,cellWidth,cellHeight,cols,rows)
      val r = run(CreateRaster(extent))

      r.rasterExtent should be (extent)
      r.cols should be (cols)
      r.rows should be (rows)
      r.get(cols/2,rows/2) should be (NODATA)
    }
  }
}
