package geotrellis.raster.op.local

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class MeanSpec extends FunSpec 
                  with ShouldMatchers 
                  with TestServer 
                  with RasterBuilders {
  describe("Mean") {
    it("takes mean on rasters of all one value") {
      val r1 = createRaster(Array.fill(7*8)(1), 7, 8)
      val r2 = createRaster(Array.fill(7*8)(5), 7, 8)
      val r3 = createRaster(Array.fill(7*8)(10), 7, 8)

      assertEqual(Mean(r1,r2,r3),Array.fill(7*8)((1+5+10)/3))
    }

    it("takes mean on rasters of varying values") {
      val n = NODATA
      val r1 = createRaster(Array(
        n, 1, n, 1, n, 1,
        2, n, 2, n, 2, n,
        n, 3, n, 3, n, 3,
        4, n, 4, n, 4, n,
        n, 5, n, 5, n, 5), 6,5)

      val r2 = createRaster(Array(
        1, n, n, n, n, 10,
        1, n, n, n, n, 9,
        1, n, n, n, n, 8,
        1, n, n, n, n, 7,
        1, n, n, n, n, 6), 6,5)

      val r3 = createRaster(Array(
        n, 8, n, 9, n, 1,
        n, n, n, 7, 2, n,
        n, 8, n, 5, 2, n,
        n, 8, n, 3, 4, n,
        n, 8, n, 1, n, n), 6,5)

      val expected = Array(
              1, (8+1)/2, n, (9+1)/2,       n, (1+10+1)/3,
        (2+1)/2,       n, 2,       7, (2+2)/2,          9,
              1, (8+3)/2, n, (3+5)/2,       2,    (8+3)/2,
        (4+1)/2,       8, 4,       3, (4+4)/2,          7,
              1, (8+5)/2, n, (5+1)/2,       n,    (6+5)/2)

      assertEqual(Mean(r1,r2,r3),expected)
    }
  }
}
