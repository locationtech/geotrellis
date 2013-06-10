package geotrellis.io

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class LoadRasterLayerInfoSpec extends FunSpec 
                                 with ShouldMatchers 
                                 with TestServer {
  describe("LoadRasterLayerInfo") {
    it("loads a cached raster.") {
      val info = run(LoadRasterLayerInfo("mtsthelens_tiled_cached"))
      info.cached should be (true)
    }
  }
}
