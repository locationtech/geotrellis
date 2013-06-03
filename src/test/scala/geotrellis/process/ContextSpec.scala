package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import geotrellis._
import geotrellis.testutil._

class ContextSpec extends FunSpec with MustMatchers {

describe("A Context") {
    it("should use caching for LoadRaster") {
      val path = "src/test/resources/quadborder8.arg"
      val context = new Context(TestServer.server)
      val geo = RasterExtent(Extent(-9.5, 3.8, 150.5, 163.8), 8.0, 8.0, 20, 20)

      val r1 = context.loadRaster(path, geo)
      val r2 = context.loadRaster(path, geo)

      r1 must be === r2
    }
  }
}
