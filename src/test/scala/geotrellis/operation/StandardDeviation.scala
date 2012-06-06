package geotrellis.operation

import geotrellis.process._
import geotrellis.operation._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers
import org.scalatest.matchers.ShouldMatchers

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DeviationRasterSpec extends Spec with MustMatchers with ShouldMatchers {
  val server = TestServer()

  describe("The DeviationRaster operation") {
    it("should work") {
      val r1 = LoadFile("src/test/resources/quad8.arg")
      val r2 = LoadFile("src/test/resources/quad8.arg")
      val h = BuildMapHistogram(r1)
      val s = StandardDeviation(r2, h, 1000)
      val raster = server.run(s)
  
      raster.data.asArray(0) must be === -1341
      raster.data.asArray(10) must be === -447
      raster.data.asArray(200) must be === 447
      raster.data.asArray(210) must be === 1341
    }
  }
}
