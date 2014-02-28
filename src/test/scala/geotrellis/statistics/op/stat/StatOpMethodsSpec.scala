package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.raster.op._
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import geotrellis.testutil.TestServer
import geotrellis.source.RasterSource
import geotrellis.statistics.Statistics

class StatOpMethodsSpec extends FunSpec with TestServer with ShouldMatchers {
  def rasterSource = RasterSource("quad_tiled")

  describe("StatOpMethods") {
    it("should calculate .statistics()") {
      val statSource = rasterSource.statistics()
      val stats = statSource.get

      val dev = math.sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats should be (expected)
    }
  }
}
