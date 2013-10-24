package geotrellis.raster.op

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class ConvertTypeSpec extends FunSpec 
                         with ShouldMatchers 
                         with TestServer 
                         with RasterBuilders {
  describe("ConvertType") {
    it("should convert a byte raster to an int raster") { 
      val r = byteRaster
      var result = run(local.Add(ConvertType(r,TypeShort),100))
      result.rasterType should be (TypeShort)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 100)
        }
      }
    }
  }
}
