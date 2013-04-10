package geotrellis.process

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

import geotrellis.{Extent,RasterExtent}
import geotrellis.testutil._

class ServerSpec extends FunSpec with MustMatchers {

describe("A Server") {

    it("should use caching for LoadRaster") {
      val path = "src/test/resources/quadborder8.arg"
      val server = TestServer.server
      val geo = RasterExtent(Extent(-9.5, 3.8, 150.5, 163.8), 8.0, 8.0, 20, 20)

      val r1 = server.loadRaster(path, geo)
      val r2 = server.loadRaster(path, geo)

      r1 must be === r2
    }
  }

}
