package geotrellis.raster.op.local

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class UndefinedSpec extends FunSpec with ShouldMatchers 
                                    with TestServer 
                                    with RasterBuilders {
  describe("Undefined") {
    it("should work with TypeInt raster") {
      val r = createRaster(Array(1,2,NODATA,NODATA,5,6,7))
      assertEqual(Undefined(r),Array(0,0,1,1,0,0,0))
    }

    it("should work with TypeDouble raster") {
      val r = createRaster(Array(1.0,2.0,Double.NaN,Double.NaN,5.0,6.0,7.0))
      assertEqual(Undefined(r),Array(0,0,1,1,0,0,0))
    }
  }
}
