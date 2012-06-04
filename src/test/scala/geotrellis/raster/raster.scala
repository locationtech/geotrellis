package geotrellis.raster

import geotrellis._

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
    val raster = Raster(data, g)

    it("should preserve the data") {
      raster.asArray must be === data
    }

    it("should get coordinate values") {
      raster.get(0, 0) must be === 1
    }

    it("should create empty rasters") {
      val r = Raster.empty(g)
      for(i <- 0 until g.cols * g.rows) {
        r.data(i) must be === NODATA
      }
    }

    it("should be comparable to others") {
      val r0:Raster = null
      val r1 = Raster(Array(1,2,3,4), g)
      val r2 = Raster(Array(1,2,3,5), g)
      val r3 = Raster(Array(1,2,3,4), g)
      val r4 = Raster(Array(1,2,3,4), g)

      r1 must not be r0
      r1 must be === r1
      r1 must not be r2
      r1 must be === r3
      r1 must be === r4
    }

    it("should set coordinates") {
      raster.set(0, 0, 10)
      raster.get(0, 0) must be === 10
      raster.set(0, 0, 1)
      raster.get(0, 0) must be === 1
    }

  }
}
