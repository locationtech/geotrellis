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
      val r1 = LoadFile("src/test/resources/quad.arg")
      val r2 = LoadFile("src/test/resources/quad.arg")
      val h = BuildMapHistogram(r1)
      val s = StandardDeviation(r2, h, 1000)
      val raster = server.run(s)
  
      raster.data(0) must be === -1341
      raster.data(10) must be === -447
      raster.data(200) must be === 447
      raster.data(210) must be === 1341
    }
  }
}
