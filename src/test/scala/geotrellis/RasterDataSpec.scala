package geotrellis

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterDataSpec extends FunSpec 
                        with ShouldMatchers 
                        with TestServer 
                        with RasterBuilders {
  describe("convert") {
    it("should throw exception when converting down") { 
      val r = positiveIntegerNoDataRaster
      intercept[IllegalArgumentException] {
        r.convert(TypeBit)
      }
    }

    it("should convert a byte raster to an int raster") { 
      val r = byteRaster
      var result = r.convert(TypeShort)
                    .map { z => z + 100 }
      result.data.getType should be (TypeShort)
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (r.get(col,row) + 100)
        }
      }
    }
  }
}
