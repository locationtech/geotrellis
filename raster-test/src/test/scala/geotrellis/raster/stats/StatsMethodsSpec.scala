package geotrellis.raster.stats

import geotrellis.raster._
import geotrellis.engine._

import geotrellis.testkit._
import org.scalatest._

class StatsMethodsSpec extends FunSpec 
                          with TestEngine
                          with Matchers {
  describe("StatsMethods") {
    it("gets expected class breaks from test raster.") {
      val testRaster = {
        val nd = NODATA
        val data1 = Array(12, 12, 13, 14, 15,
          44, 91, nd, 11, 95,
          12, 13, 56, 66, 66,
          44, 91, nd, 11, 95)
        IntArrayTile(data1, 5, 4)
      }

      testRaster.classBreaks(4) should be (Array(12, 15, 66, 95))
    }

    it("standard deviation should match known values from quad8 raster") {
      val r = RasterSource.fromPath("raster-test/data/quad8.arg").get
      val std = r.standardDeviations(1000)

      val d = std.toArray
  
      d(0) should be (-1341)
      d(10) should be (-447)
      d(200) should be (447)
      d(210) should be (1341)
    }

    it("get expected statistics from quad8") {
      val stats = RasterSource.fromPath("raster-test/data/quad8.arg").get.statistics

      val dev = math.sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats should be (expected)
    }

    it("should get correct histogram values from test raster.") {
      val testRaster = {
        val nd = NODATA
        val data1 = 
          Array(
            12, 12, 13, 14, 15,
            44, 91, nd, 11, 95,
            12, 13, 56, 66, 66,
            44, 91, nd, 11, 95
          )
        IntArrayTile(data1, 5, 4)
      }
      val histo = testRaster.histogram

      histo.getTotalCount should be (18)
      histo.getItemCount(11) should be (2)
      histo.getItemCount(12) should be (3)

      histo.getQuantileBreaks(4) should be (Array(12, 15, 66, 95))
    }
  }
}
