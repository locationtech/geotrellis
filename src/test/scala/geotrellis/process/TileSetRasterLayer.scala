package geotrellis.process

import geotrellis._
import geotrellis.testutil._

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers


class TileSetRasterLayerSpec extends FunSpec
                                with ShouldMatchers 
                                with TestServer {
  describe("Loading a tile layer through the server") {
    it("should match the non-tiled version of the same raster") {
      val tiled = run(io.LoadRaster("tile_SBN_inc_percap"))
      val nontiled = run(io.LoadFile("src/test/resources/sbn/SBN_inc_percap.json"))

      println(s"${tiled.rasterExtent.extent}")
      println(s"${nontiled.rasterExtent.extent}")
      tiled.rasterExtent.extent.containsExtent(nontiled.rasterExtent.extent) should be (true)
    }
  }
}
