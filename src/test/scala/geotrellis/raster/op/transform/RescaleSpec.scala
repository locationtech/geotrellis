package geotrellis.raster.op.transform

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RescaleSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer {
  describe("RescaleRaster") {
    it("should resize quad8 correctly") {

      // double number of rows and cols
      val re = RasterExtent(Extent(-9.5,3.8,150.5,163.8),4.0,4.0,40,40)
      val raster = run(Rescale(io.LoadFile("src/test/resources/quad8.arg"), 2.0))

      raster.cols should be (40)
      raster.rows should be (40)
      raster.rasterExtent should be (re)
      
      val d = raster.data.asArray
      println(raster.asciiDraw())
      println(raster.rasterExtent)
      raster.get(0,0) should be (1)
      raster.get(21,0) should be (2)
      raster.get(0,21) should be (3)
      raster.get(21,21) should be (4)
    }
  }
}
