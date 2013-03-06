package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class AndSpec extends FunSpec 
                 with ShouldMatchers 
                 with TestServer 
                 with RasterBuilders {
  describe("And") {
    it("ands two integers") { 
      run(And(345,543)) should be (25)
    }

    it("ands an Int raster and a constant") {
      assertEqual(And(createValueRaster(10,9),3), createValueRaster(10,1))
    }

    it("ands two Int rasters") {
      assertEqual(And(createValueRaster(10,9),createValueRaster(10,3)), 
                  createValueRaster(10,1))
    }

    it("ands a Double raster and a constant") {
      assertEqual(And(createValueRaster(10,9.4),3), createValueRaster(10,1.0))
    }

    it("ands two Double rasters") {
      assertEqual(And(createValueRaster(10,9.9),createValueRaster(10,3.2)), 
                  createValueRaster(10,1.0))
    }
  }
}
