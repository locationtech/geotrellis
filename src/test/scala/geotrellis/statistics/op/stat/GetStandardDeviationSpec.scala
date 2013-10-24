package geotrellis.statistics.op.stat

import geotrellis._
import geotrellis.io.LoadFile
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class GetStandardDeviationSpec extends FunSpec 
                                  with TestServer
                                  with ShouldMatchers {
  describe("GetStandardDeviation") {
    it("should match known values from quad8 raster") {
      val r = run(LoadFile("src/test/resources/quad8.arg"))
      val std = run(GetStandardDeviation(r, GetHistogram(r), 1000))

      val d = std.toArray
  
      d(0) should be (-1341)
      d(10) should be (-447)
      d(200) should be (447)
      d(210) should be (1341)
    }
  }
}
