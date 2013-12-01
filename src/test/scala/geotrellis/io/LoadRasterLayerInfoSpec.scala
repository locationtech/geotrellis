package geotrellis.io

import geotrellis._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import geotrellis.testutil._

class LoadRasterLayerInfoSpec extends FunSpec 
                                 with ShouldMatchers 
                                 with TestServer {
  describe("LoadRasterLayerInfo") {
    it("loads a cached raster.") {
      val info = get(LoadRasterLayerInfo("mtsthelens_tiled_cached"))
      info.cached should be (true)
    }

    it("loads a raster with a data store.") {
      val info = get(LoadRasterLayerInfo("test:fs","quadborder"))
      val info2 = get(LoadRasterLayerInfo("test:fs2","quadborder"))

      info.rasterExtent should not be (info2.rasterExtent)
    }
  }
}
