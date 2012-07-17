package geotrellis.op

import geotrellis.process._
import geotrellis.op._
import geotrellis.op.raster._
import geotrellis.op.stat._

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
      val h = stat.MapHistogram(r1)
      val s = StandardDeviation(r2, h, 1000)
      val raster = server.run(s)

      val d = raster.data.asArray.getOrElse(sys.error("argh"))
  
      d(0) must be === -1341
      d(10) must be === -447
      d(200) must be === 447
      d(210) must be === 1341
    }
  }
}
