package geotrellis.raster.op.transform

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ResampleRasterSpec extends FunSpec 
                            with ShouldMatchers 
                            with TestServer {
  describe("ResampleRaster") {
    it("should resample quad8 correctly") {
      val raster = run(ResampleRaster(io.LoadFile("src/test/resources/quad8.arg"), 4, 4))

      raster.cols should be (4)
      raster.rows should be (4)

      val d = raster.data.asArray.getOrElse(sys.error("Can't get RasterData as Array."))

      d(0) should be (1)
      d(3) should be (2)
      d(8) should be (3)
      d(11) should be (4)
    }
  }
}
