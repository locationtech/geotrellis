package trellis.raster

import trellis.{Extent,RasterExtent}
import trellis.constant._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class RasterSpec extends Spec with MustMatchers {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val g = RasterExtent(e, 1.0, 1.0, 10, 10)
  describe("A Raster") {
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val raster = IntRaster(data, rows=3, cols=3, rasterExtent=g)

    it("should preserve the data") {
      raster.asArray must be === data
    }

    it("should get coordinate values") {
      raster.get(0, 0) must be === 1
    }

    it("should create empty rasters") {
      val r = IntRaster.createEmpty(g)
      for(i <- 0 until g.cols * g.rows) {
        r.data(i) must be === NODATA
      }
    }

    it("should be comparable to others") {
      val r0:IntRaster = null
      val r1 = IntRaster(Array(1,2,3,4), 2, 2, g)
      val r2 = IntRaster(Array(1,2,3,5), 2, 2, g)
      val r3 = IntRaster(Array(1,2,3,4), 1, 4, g)
      val r4 = IntRaster(Array(1,2,3,4), 4, 1, g)

      r1 == r0 must be === false
      r1 == r1 must be === true
      r1 == r2 must be === false
      r1 == r3 must be === false
      r1 == r4 must be === false
    }

    it("should set coordinates") {
      raster.set(0, 0, 10)
      raster.get(0, 0) must be === 10
      raster.set(0, 0, 1)
      raster.get(0, 0) must be === 1
    }

  }
}
