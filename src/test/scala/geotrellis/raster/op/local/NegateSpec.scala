package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class NegateSpec extends FunSpec 
                    with ShouldMatchers 
                    with TestServer 
                    with RasterBuilders {
  describe("Negate") {
    it("negates an integer raster") {
      val r = positiveIntegerRaster
      val result = run(Negate(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.get(col,row) should be (-r.get(col,row))
        }
      }
    }

    it("negates a double raster") {
      val r = probabilityRaster
      val result = run(Negate(r))
      for(col <- 0 until r.cols) {
        for(row <- 0 until r.rows) {
          result.getDouble(col,row) should be (-r.getDouble(col,row))
        }
      }
    }
  }
}
