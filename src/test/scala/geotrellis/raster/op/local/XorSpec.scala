package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class XorSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Xor") {
    it("xors two integers") { 
      run(Xor(345,543)) should be (838)
    }

    it("xors an Int raster xor a constant") {
      assertEqual(Xor(createValueRaster(10,9),3), createValueRaster(10,10))
    }

    it("xors two Int rasters") {
      assertEqual(Xor(createValueRaster(10,9),createValueRaster(10,3)), 
                  createValueRaster(10,10))
    }

    it("xors a Double raster xor a constant") {
      assertEqual(Xor(createValueRaster(10,9.4),3), createValueRaster(10,10.0))
    }

    it("xors two Double rasters") {
      assertEqual(Xor(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,10.0))
    }
  }
}
