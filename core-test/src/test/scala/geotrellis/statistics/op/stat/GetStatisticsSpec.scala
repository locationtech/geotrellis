package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.statistics.Statistics
import geotrellis.io.LoadFile

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testkit._

class GetStatisticsSpec extends FunSpec 
                           with TestServer
                           with ShouldMatchers {
  describe("GetStatistics") {
    it("get expected statistics from quad8") {
      val r = io.LoadFile("core-test/data/quad8.arg")
      val stats = get(GetStatistics(GetHistogram(r)))

      val dev = math.sqrt((2 * (0.5 * 0.5) + 2 * (1.5 * 1.5)) / 4)
      val expected = Statistics(2.5, 3, 1, dev, 1, 4)

      stats should be (expected)
    }
  }
}
