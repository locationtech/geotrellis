package geotrellis

import org.scalatest.FunSpec
import org.scalatest.matchers._

class RasterSpec extends FunSpec with MustMatchers 
                                 with ShouldMatchers {
  val e = Extent(0.0, 0.0, 10.0, 10.0)
  val g = RasterExtent(e, 1.0, 1.0, 10, 10)
  describe("A Raster") {
    val data = Array(1, 2, 3,
                     4, 5, 6,
                     7, 8, 9)
    val raster = Raster(data, g)

    it("should preserve the data") {
      raster.toArray must be === data
    }

    it("should get coordinate values") {
      raster.get(0, 0) must be === 1
    }

    it("should create empty rasters") {
      val r = Raster.empty(g)
      val d = r.toArray
      for(i <- 0 until g.cols * g.rows) {
        d(i) must be === NODATA
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

    it("should normalize from 500-999 to 1-100") {
      val arr = (for(i <- 500 to 999) yield { i }).toArray
      val r = Raster(arr, RasterExtent(Extent(0.0,0.0,50.0,10.0), 1, 1, 50, 10))
      val (oldMin, oldMax) = r.findMinMax
      val nr = r.normalize(oldMin, oldMax, 1, 100)
      val (newMin, newMax) = nr.findMinMax

      newMin should be (1)
      newMax should be (100)
      nr.toArray.toSet should be ((for(i <- 1 to 100) yield { i }).toSet)
    }
  }
}
