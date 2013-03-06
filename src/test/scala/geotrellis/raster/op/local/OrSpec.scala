package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class OrSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("Or") {
    it("ors two integers") { 
      run(Or(345,543)) should be (863)
    }

    it("ors an Int raster or a constant") {
      assertEqual(Or(createValueRaster(10,9),3), createValueRaster(10,11))
    }

    it("ors two Int rasters") {
      assertEqual(Or(createValueRaster(10,9),createValueRaster(10,3)), 
                  createValueRaster(10,11))
    }

    it("ors a Double raster or a constant") {
      assertEqual(Or(createValueRaster(10,9.4),3), createValueRaster(10,11.0))
    }

    it("ors two Double rasters") {
      assertEqual(Or(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,11.0))
    }
  }
}
