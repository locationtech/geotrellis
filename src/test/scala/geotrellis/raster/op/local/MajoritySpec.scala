package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class MajoritySpec extends FunSpec 
                  with ShouldMatchers 
                  with TestServer 
                  with RasterBuilders {
  describe("Majority") {
    it("takes mean on rasters of all one value") {
      val r1 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r2 = createRaster(Array.fill(7*8)(5), 7, 8)
      val r3 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r4 = createRaster(Array.fill(7*8)(7), 7, 8)
      val r5 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r6 = createRaster(Array.fill(7*8)(7), 7, 8)
      val r7 = createRaster(Array.fill(7*8)(NODATA), 7, 8)

      assertEqual(Majority(r1,r2,r3,r4,r5,r6,r7), Array.fill(7*8)(1))
      assertEqual(Majority(1,r1,r2,r3,r4,r5,r6), Array.fill(7*8)(7))
      assertEqual(Majority(Array(r1,r2,r3,r4,r5,r6),2), Array.fill(7*8)(5))
      assertEqual(Majority(3,r1,r2,r3), Array.fill(7*8)(NODATA))
      assertEqual(Majority(4,r1,r2,r3), Array.fill(7*8)(NODATA))
    }
  }
}
